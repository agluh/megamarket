package com.github.agluh.megamarket.dto;

import com.github.agluh.megamarket.model.ShopUnitStatistic;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ShopUnitStatisticResponse {

    private Collection<ShopUnitStatistic> items;
}
