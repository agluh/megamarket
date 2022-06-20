package com.github.agluh.megamarket.service;

import com.github.agluh.megamarket.dto.ShopUnit;
import com.github.agluh.megamarket.dto.ShopUnitImport;
import com.github.agluh.megamarket.dto.ShopUnitStatistic;
import com.github.agluh.megamarket.model.Category;
import com.github.agluh.megamarket.model.Offer;
import com.github.agluh.megamarket.repository.CategoryRepository;
import com.github.agluh.megamarket.repository.OfferRepository;
import com.github.agluh.megamarket.repository.ShopUnitReadModel;
import com.github.agluh.megamarket.repository.ShopUnitStatisticReadModel;
import com.github.agluh.megamarket.service.exceptions.IdentityIsNotUniqueException;
import com.github.agluh.megamarket.service.exceptions.ShopUnitNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides management for catalog related tasks.
 */
@Service
@AllArgsConstructor
public class ShopService {

    private final OfferRepository offerRepository;
    private final CategoryRepository categoryRepository;
    private final ShopUnitReadModel shopUnitReadModel;
    private final ShopUnitStatisticReadModel statisticReadModel;

    /**
     * Imports data into catalog.
     *
     * <p>For the best performance we do some logic on application side
     * before sending data to the storage. Firstly we separate offers
     * and categories. Then we're sorting imported categories so that
     * all dependent categories being stored after their parents.
     * After that we can safely store offers too. We're also keep
     * list of categories being affected by this import, and then
     * we'll run batch updating of prices and dates on them.
     * If an element is changes their parent, we store id of previous parent
     * in as separate field in the storage, so afterwards we can update them too.
     * All elements being affected by this import operation (both implicitly or explicitly)
     * will get an updated date field.
     *
     * @param items collection of items to be imported
     * @param updateDate date of importing
     * @throws IdentityIsNotUniqueException in case ids are not unique
     */
    @Transactional
    public void importData(Collection<ShopUnitImport> items, Instant updateDate) {
        ensureIdUniqueness(items);

        Collection<Category> cats = items.stream()
            .filter(ShopUnitImport::isCategory)
            .map(e -> e.toCategory(updateDate))
            .collect(Collectors.toSet());

        Collection<Offer> offers = items.stream()
            .filter(ShopUnitImport::isOffer)
            .map(e -> e.toOffer(updateDate))
            .collect(Collectors.toSet());

        final Set<UUID> affectedCatsIds = getCategoriesWithUpdatedPrice(offers, cats);

        categoryRepository.save(orderByDependency(cats));

        offerRepository.save(offers);

        updateCategoriesPrice(affectedCatsIds, updateDate);
    }

    /**
     * Retrieves an element of catalog by its identity.
     *
     * <p>If the element is a category, then all subcategories
     * and products from them are fetched too.
     *
     * @return fetched node
     * @throws ShopUnitNotFoundException in case element not found by its identity
     */
    public ShopUnit getNode(UUID nodeId) {
        Collection<ShopUnit> nodes = shopUnitReadModel.getNodeWithSubtree(nodeId);
        if (nodes.isEmpty()) {
            throw new ShopUnitNotFoundException();
        }

        return buildTree(nodes);
    }

    /**
     * Deletes a node by its identity.
     *
     * <p>If the element is a category, then all subcategories
     * and products from them are deleted too.
     * All upstream categories being affected by this operation
     * will update their prices accordingly.
     * This operation not changes last update date of any elements.
     *
     * @throws ShopUnitNotFoundException in case element not found by its identity
     */
    @Transactional
    public void deleteNode(UUID nodeId) {
        ShopUnit node = shopUnitReadModel.getNode(nodeId)
            .orElseThrow(ShopUnitNotFoundException::new);

        if (node.isCategory()) {
            categoryRepository.delete(node.getId());
        } else {
            offerRepository.delete(node.getId());
        }

        UUID parentId = node.getParentId();
        if (parentId != null) {
            updateCategoriesPrice(List.of(parentId), null);
        }
    }

    /**
     * Returns list of offers that being updated in last 24 hours from {@code now}.
     */
    public Collection<ShopUnit> getSales(Instant now) {
        Instant from = now.minus(24, ChronoUnit.HOURS);
        return shopUnitReadModel.getOffersUpdatedBetween(from, now);
    }

    /**
     * Returns list of node statistics.
     */
    public Collection<ShopUnitStatistic> getNodeStatistics(UUID nodeId, Instant fromIncluding,
            Instant toExcluding) {
        shopUnitReadModel.getNode(nodeId).orElseThrow(ShopUnitNotFoundException::new);
        return statisticReadModel.getNodeStatistics(nodeId, fromIncluding, toExcluding);
    }

    /**
     * Builds a tree from flat collection of elements.
     *
     * @return root of the tree
     * @throws IdentityIsNotUniqueException in case ids are not unique
     */
    private ShopUnit buildTree(Collection<ShopUnit> flatList) {
        Map<UUID, ShopUnit> map = new HashMap<>();

        flatList.forEach(item -> {
            if (map.containsKey(item.getId())) {
                throw new IdentityIsNotUniqueException();
            } else {
                map.put(item.getId(), item);
            }
        });

        for (ShopUnit s : map.values()) {
            UUID parentId = s.getParentId();
            if (parentId == null || !map.containsKey(parentId)) {
                s.setTopLevel(true);
                continue;
            }

            ShopUnit parent = map.get(parentId);
            parent.addChild(s);
        }

        return map.values().stream()
            .filter(ShopUnit::isTopLevel)
            .findFirst().get();
    }

    /**
     * Reorders collection so that parent elements goes before any of their children.
     */
    private Collection<Category> orderByDependency(Collection<Category> categories) {
        class Node<T> {
            final T data;
            final List<Node<T>> children = new ArrayList<>();
            boolean isTopLevel = false;

            public Node(T data) {
                this.data = data;
            }

            public boolean isTopLevel() {
                return isTopLevel;
            }
        }

        Map<UUID, Node<Category>> map =
            categories.stream().collect(Collectors.toMap(Category::getId, Node::new));

        for (Node<Category> n : map.values()) {
            UUID parentId = n.data.getParentId();
            if (parentId == null || !map.containsKey(parentId)) {
                n.isTopLevel = true;
                continue;
            }

            Node<Category> parent = map.get(parentId);
            parent.children.add(n);
        }

        List<Node<Category>> topNodes = map.values().stream()
            .filter(Node::isTopLevel)
            .toList();

        Queue<Node<Category>> queue = new LinkedList<>(topNodes);
        List<Category> orderedCategories = new LinkedList<>();

        while (!queue.isEmpty()) {
            Node<Category> n = queue.poll();
            orderedCategories.add(n.data);
            queue.addAll(n.children);
        }

        return orderedCategories;
    }

    /**
     * Checks if all IDs in importing collection are unique.
     */
    private void ensureIdUniqueness(Collection<ShopUnitImport> items) {
        Map<UUID, ShopUnitImport> map = new HashMap<>();

        items.forEach(item -> {
            if (map.containsKey(item.getId())) {
                throw new IdentityIsNotUniqueException();
            } else {
                map.put(item.getId(), item);
            }
        });
    }

    /**
     * Updates price and date (if passed) for list of collection IDs.
     */
    private void updateCategoriesPrice(Collection<UUID> catsIds, @Nullable Instant updateDate) {
        Collection<Category> categories =
            categoryRepository.getAllUpstreamCategories(catsIds);

        for (Category c : categories) {
            Long avgPrice = offerRepository.getAvgPriceOfCategory(c.getId());
            c.setPrice(avgPrice);

            if (updateDate != null) {
                c.setDate(updateDate);
            }
        }

        categoryRepository.save(categories);
    }

    /**
     * Returns a list of IDs of categories that are affected in some way by
     * offers importing.
     *
     * <p>Category consider affected in case:
     * <ul>
     * <li>it has new offers</li>
     * <li>some offers has been removed from it</li>
     * <li>price of any offer has been changed</li>
     * </ul>
     */
    private Set<UUID> getCategoriesWithUpdatedPrice(Collection<Offer> importingOffers,
            Collection<Category> importingCategories) {
        Collection<Offer> oldOffers = offerRepository.findByIds(
            importingOffers.stream().map(Offer::getId).toList());
        Map<UUID, Offer> oldOffersMap =
            oldOffers.stream().collect(Collectors.toMap(Offer::getId, Function.identity()));
        List<UUID> affectedCats = new ArrayList<>();

        for (Offer o : importingOffers) {
            UUID parentId = o.getParentId();

            if (oldOffersMap.containsKey(o.getId())) {
                Offer old = oldOffersMap.get(o.getId());
                UUID oldParentId = old.getParentId();

                if (Objects.equals(parentId, oldParentId)) {
                    if (o.getPrice() != old.getPrice()) {
                        affectedCats.add(parentId);
                    }
                } else {
                    affectedCats.add(parentId);
                    affectedCats.add(oldParentId);
                }
            } else {
                affectedCats.add(parentId);
            }
        }

        Collection<Category> oldCats = categoryRepository.findByIds(
            importingCategories.stream().map(Category::getId).toList());
        Map<UUID, Category> oldCatsMap =
            oldCats.stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        for (Category c : importingCategories) {
            UUID parentId = c.getParentId();

            if (oldCatsMap.containsKey(c.getId())) {
                Category old = oldCatsMap.get(c.getId());
                UUID oldParentId = old.getParentId();

                if (!Objects.equals(parentId, oldParentId) && old.getPrice() != null) {
                    affectedCats.add(c.getId());
                    affectedCats.add(oldParentId);
                }
            }
        }

        return affectedCats.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
