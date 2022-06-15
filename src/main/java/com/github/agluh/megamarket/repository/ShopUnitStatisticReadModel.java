package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.ShopUnitStatistic;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Read model for statistic of a catalog item.
 */
public interface ShopUnitStatisticReadModel {

    /**
     * Returns list of statistics elements for catalog item.
     */
    Collection<ShopUnitStatistic> getNodeStatistics(UUID nodeId, Instant fromIncluding,
        Instant toExcluding);
}

