package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.ShopUnitStatistic;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface ShopUnitStatisticRepository {

    Collection<ShopUnitStatistic> getNodeStatistics(UUID nodeId, Instant fromIncluding,
        Instant toExcluding);
}

