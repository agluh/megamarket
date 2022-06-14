package com.github.agluh.megamarket.service;

import com.github.agluh.megamarket.dto.ShopUnitImport;
import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.model.ShopUnitStatistic;
import com.github.agluh.megamarket.repository.ShopUnitRepository;
import com.github.agluh.megamarket.repository.ShopUnitStatisticRepository;
import com.github.agluh.megamarket.service.exceptions.IdentityIsNotUniqueException;
import com.github.agluh.megamarket.service.exceptions.ShopUnitNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Provides management for shop related tasks.
 */
@Service
@AllArgsConstructor
public class ShopService {

    private final ShopUnitRepository unitRepository;

    private final ShopUnitStatisticRepository statisticRepository;

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
        List<ShopUnit> nodes = items.stream()
            .map(e -> e.toModel(updateDate))
            .toList();

        List<ShopUnit> categories = nodes.stream()
            .filter(ShopUnit::isCategory)
            .toList();

        Collection<ShopUnit> categoriesTree = buildTree(categories);
        List<ShopUnit> orderedCategories = flattenTree(categoriesTree);

        if (!orderedCategories.isEmpty()) {
            unitRepository.saveCategories(orderedCategories);
        }

        List<ShopUnit> offers = nodes.stream()
            .filter(ShopUnit::isOffer)
            .toList();

        if (!offers.isEmpty()) {
            unitRepository.saveOffers(offers);
        }

        Set<UUID> affectedCategoriesIds = offers.stream()
            .map(ShopUnit::getParentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (!affectedCategoriesIds.isEmpty()) {
            unitRepository.updateCategoriesPricesAndDates(affectedCategoriesIds, updateDate);
        }

        unitRepository.updatePricesAndDatesOfOrphanedCategories(updateDate);
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
        Collection<ShopUnit> nodes = unitRepository.getNodeWithSubtree(nodeId);
        if (nodes.isEmpty()) {
            throw new ShopUnitNotFoundException();
        }

        Collection<ShopUnit> tree = buildTree(nodes);
        assert (tree.size() == 1);

        return tree.iterator().next();
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
        ShopUnit node = unitRepository.getNode(nodeId)
            .orElseThrow(ShopUnitNotFoundException::new);

        unitRepository.deleteNode(node);

        UUID parentId = node.getParentId();
        if (parentId != null) {
            unitRepository.updateUpstreamCategoriesPricesStartingWith(parentId);
        }
    }

    /**
     * Returns list of offers that being updated in last 24 hours from {@code now}.
     */
    public Collection<ShopUnit> getSales(Instant now) {
        Instant from = now.minus(24, ChronoUnit.HOURS);
        return unitRepository.getOffersUpdatedBetween(from, now);
    }

    public Collection<ShopUnitStatistic> getNodeStatistics(UUID nodeId, Instant fromIncluding,
            Instant toExcluding) {
        unitRepository.getNode(nodeId).orElseThrow(ShopUnitNotFoundException::new);
        return statisticRepository.getNodeStatistics(nodeId, fromIncluding, toExcluding);
    }

    /**
     * Builds a tree from flat list of elements.
     *
     * @return collection of top level nodes from the tree
     * @throws IdentityIsNotUniqueException in case ids are not unique
     */
    private Collection<ShopUnit> buildTree(Collection<ShopUnit> flatList) {
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
            .collect(Collectors.toList());
    }

    private List<ShopUnit> flattenTree(Collection<ShopUnit> categoriesTree) {
        Queue<ShopUnit> queue = new LinkedList<>(categoriesTree);
        List<ShopUnit> orderedCategories = new LinkedList<>();

        while (!queue.isEmpty()) {
            ShopUnit s = queue.poll();
            orderedCategories.add(s);
            queue.addAll(s.getChildren());
        }

        return orderedCategories;
    }
}
