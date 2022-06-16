package com.github.agluh.megamarket.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.agluh.megamarket.serialization.ShopUnitSerializer;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents an item of catalog (both category and offer).
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonSerialize(using = ShopUnitSerializer.class)
public class ShopUnit {

    @EqualsAndHashCode.Include
    private final UUID id;

    private final UUID parentId;

    private final String name;

    private final Long price;

    private final Instant date;

    private final ShopUnitType type;

    @Setter
    private boolean isTopLevel;

    private final Set<ShopUnit> children = new HashSet<>();

    public void addChild(ShopUnit child) {
        children.add(child);
    }

    public boolean isCategory() {
        return type == ShopUnitType.CATEGORY;
    }

    public boolean isOffer() {
        return type == ShopUnitType.OFFER;
    }
}
