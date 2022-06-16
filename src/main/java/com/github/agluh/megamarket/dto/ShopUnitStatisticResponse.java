package com.github.agluh.megamarket.dto;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO for catalog item statistics response.
 */
@AllArgsConstructor
@Getter
public class ShopUnitStatisticResponse {

    private Collection<ShopUnitStatistic> items;
}
