package com.github.agluh.megamarket.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.agluh.megamarket.service.InstantSerializer;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents statistics of catalog item.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShopUnitStatistic {

    @EqualsAndHashCode.Include
    private final UUID id;

    private final UUID parentId;

    private final String name;

    private final Long price;

    @JsonSerialize(using = InstantSerializer.class)
    private final Instant date;

    private final ShopUnitType type;
}
