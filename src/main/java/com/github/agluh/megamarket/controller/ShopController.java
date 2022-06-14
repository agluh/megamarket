package com.github.agluh.megamarket.controller;

import com.github.agluh.megamarket.dto.ShopUnitImportRequest;
import com.github.agluh.megamarket.dto.ShopUnitStatisticResponse;
import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.service.ShopService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ShopController {

    public static final String ISO8601_DATE_TIME = "uuuu-MM-dd'T'HH:mm:ss[.SSS][XXX][XX][X]";

    private final ShopService shopService;

    @PostMapping("/imports")
    public void importData(@Valid @RequestBody ShopUnitImportRequest request) {
        shopService.importData(request.getItems(), request.getUpdateDate());
    }

    @DeleteMapping("/delete/{id}")
    public void deleteNode(@PathVariable("id") UUID nodeId) {
        shopService.deleteNode(nodeId);
    }

    @GetMapping("/nodes/{id}")
    public ShopUnit getNode(@PathVariable("id") UUID nodeId) {
        return shopService.getNode(nodeId);
    }

    @GetMapping("/sales")
    public Collection<ShopUnit> getSales(
            @RequestParam("date")
            @DateTimeFormat(pattern = ISO8601_DATE_TIME) OffsetDateTime date) {
        return shopService.getSales(date.toInstant());
    }

    @GetMapping("/node/{id}/statistic")
    public ShopUnitStatisticResponse get(
            @PathVariable("id") UUID nodeId,
            @RequestParam(value = "dateStart", required = false)
                @DateTimeFormat(pattern = ISO8601_DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "dateEnd", required = false)
                @DateTimeFormat(pattern = ISO8601_DATE_TIME) OffsetDateTime to) {
        Instant fromLimit = from != null ? from.toInstant() : null;
        Instant toLimit = to != null ? to.toInstant() : null;
        return new ShopUnitStatisticResponse(
            shopService.getNodeStatistics(nodeId, fromLimit, toLimit));
    }
}
