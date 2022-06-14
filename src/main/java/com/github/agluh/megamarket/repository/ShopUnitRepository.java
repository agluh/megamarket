package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopUnitRepository {

    /**
     * @throws InvalidIdentityException in case element has parentId that not exists
     */
    void saveCategories(List<ShopUnit> nodes);

    /**
     * @throws InvalidIdentityException in case element has parentId that not exists
     */
    void saveOffers(List<ShopUnit> nodes);

    Collection<ShopUnit> getNodeWithSubtree(UUID nodeId);

    Collection<ShopUnit> getOffersUpdatedBetween(Instant fromIncluding, Instant toIncluding);

    Optional<ShopUnit> getNode(UUID nodeId);

    void deleteNode(ShopUnit node);

    void updateUpstreamCategoriesPricesStartingWith(UUID categoryId);

    void updateCategoriesPricesAndDates(Collection<UUID> categoryIds, Instant updateDate);

    void updatePricesAndDatesOfOrphanedCategories(Instant updateDate);
}
