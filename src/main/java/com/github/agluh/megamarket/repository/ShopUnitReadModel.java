package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.dto.ShopUnit;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Read model for abstract shop elements.
 */
public interface ShopUnitReadModel {

    /**
     * Returns a node if any found.
     */
    Optional<ShopUnit> getNode(UUID nodeId);

    /**
     * Returns list of nodes.
     */
    Collection<ShopUnit> getNodeWithSubtree(UUID nodeId);

    /**
     * Returns list of offers updated between two dates.
     */
    Collection<ShopUnit> getOffersUpdatedBetween(Instant fromIncluding, Instant toIncluding);
}
