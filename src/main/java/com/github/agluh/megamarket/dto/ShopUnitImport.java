package com.github.agluh.megamarket.dto;

import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.model.ShopUnitType;
import com.github.agluh.megamarket.validator.ValidShopUnitImport;
import java.time.Instant;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public ShopUnit toModel(Instant updateDate) {
        return new ShopUnit(id, parentId, name, price, updateDate, type);
    }
}
