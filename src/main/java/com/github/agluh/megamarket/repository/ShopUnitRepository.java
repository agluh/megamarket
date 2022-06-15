package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for catalog items.
 */
public interface ShopUnitRepository {

    /**
     * Saves categories to the storage.
     *
     * @throws InvalidIdentityException in case element has parentId that not exists
     */
    void saveCategories(List<ShopUnit> nodes);

    /**
     * Saves offers to the storage.
     *
     * @throws InvalidIdentityException in case element has parentId that not exists
     */
    void saveOffers(List<ShopUnit> nodes);

    /**
     * Returns list of nodes.
     */
    Collection<ShopUnit> getNodeWithSubtree(UUID nodeId);

    /**
     * Returns list of offers updated between two dates.
     */
    Collection<ShopUnit> getOffersUpdatedBetween(Instant fromIncluding, Instant toIncluding);

    /**
     * Returns a node if any found.
     */
    Optional<ShopUnit> getNode(UUID nodeId);

    /**
     * Deletes a node.
     */
    void deleteNode(ShopUnit node);

    /**
     * Updates price for all upstream categories starting with {@code categoryId}.
     *
     * <p>Dates on any elements are not updated.
     */
    void updateUpstreamCategoriesPricesStartingWith(UUID categoryId);

    /**
     * Updates price and date for all upstream categories starting
     * with each node from {@code categoryIds}.
     */
    void updateCategoriesPricesAndDates(Collection<UUID> categoryIds, Instant updateDate);

    /**
     * Updates price and date in each category that has lost some of their children.
     */
    void updatePricesAndDatesOfOrphanedCategories(Instant updateDate);
}
