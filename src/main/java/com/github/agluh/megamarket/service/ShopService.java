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
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
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

    private final ShopUnitStatisticReadModel statisticRepository;

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
        List<Category> categories = items.stream()
            .filter(ShopUnitImport::isCategory)
            .map(e -> e.toCategory(updateDate))
            .toList();

        if (!categories.isEmpty()) {
            categoryRepository.save(orderByDependency(categories));
        }

        List<Offer> offers = items.stream()
            .filter(ShopUnitImport::isOffer)
            .map(e -> e.toOffer(updateDate))
            .toList();

        if (!offers.isEmpty()) {
            offerRepository.save(offers);
        }

        Set<UUID> affectedCategoriesIds = offers.stream()
            .map(Offer::getParentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (!affectedCategoriesIds.isEmpty()) {
            categoryRepository.updateCategoriesPricesAndDates(affectedCategoriesIds, updateDate);
        }

        categoryRepository.updatePricesAndDatesOfOrphanedCategories(updateDate);
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
            categoryRepository.updateUpstreamCategoriesPricesStartingWith(parentId);
        }
    }

    /**
     * Returns list of offers that being updated in last 24 hours from {@code now}.
     */
    public Collection<ShopUnit> getSales(Instant now) {
        Instant from = now.minus(24, ChronoUnit.HOURS);
        return shopUnitReadModel.getOffersUpdatedBetween(from, now);
    }

    public Collection<ShopUnitStatistic> getNodeStatistics(UUID nodeId, Instant fromIncluding,
            Instant toExcluding) {
        shopUnitReadModel.getNode(nodeId).orElseThrow(ShopUnitNotFoundException::new);
        return statisticRepository.getNodeStatistics(nodeId, fromIncluding, toExcluding);
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

        Map<UUID, Node<Category>> map = new HashMap<>();

        categories.forEach(item -> {
            if (map.containsKey(item.getId())) {
                throw new IdentityIsNotUniqueException();
            } else {
                map.put(item.getId(), new Node<>(item));
            }
        });

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
}
