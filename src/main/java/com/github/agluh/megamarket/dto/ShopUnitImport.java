package com.github.agluh.megamarket.dto;

import com.github.agluh.megamarket.model.Category;
import com.github.agluh.megamarket.model.Offer;
import com.github.agluh.megamarket.validation.ValidShopUnitImport;
import java.time.Instant;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for catalog item import.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ValidShopUnitImport
public class ShopUnitImport {

    @NotNull
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    private ShopUnitType type;

    private UUID parentId;

    private Long price;

    public boolean isCategory() {
        return type == ShopUnitType.CATEGORY;
    }

    public boolean isOffer() {
        return type == ShopUnitType.OFFER;
    }

    public Category toCategory(Instant updateDate) {
        return new Category(id, parentId, name, price, updateDate);
    }

    public Offer toOffer(Instant updateDate) {
        return new Offer(id, parentId, name, price, updateDate);
    }
}
