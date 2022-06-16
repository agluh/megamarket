package com.github.agluh.megamarket.controller;

import com.github.agluh.megamarket.dto.ShopUnit;
import com.github.agluh.megamarket.dto.ShopUnitImportRequest;
import com.github.agluh.megamarket.dto.ShopUnitStatisticResponse;
import com.github.agluh.megamarket.service.ShopService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.UUID;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller fro all endpoints related to catalog.
 */
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
    public void deleteNode(@PathVariable("id") String nodeId) {
        shopService.deleteNode(parseUuid(nodeId));
    }

    @GetMapping("/nodes/{id}")
    public ShopUnit getNode(@PathVariable("id") String nodeId) {
        return shopService.getNode(parseUuid(nodeId));
    }

    @GetMapping("/sales")
    public Collection<ShopUnit> getSales(@RequestParam("date") String date) {
        return shopService.getSales(parseInstant(date));
    }

    @GetMapping("/node/{id}/statistic")
    public ShopUnitStatisticResponse get(
            @PathVariable("id") String nodeId,
            @RequestParam(value = "dateStart", required = false) String from,
            @RequestParam(value = "dateEnd", required = false) String to) {
        return new ShopUnitStatisticResponse(
            shopService.getNodeStatistics(parseUuid(nodeId), parseInstant(from), parseInstant(to)));
    }

    private static Instant parseInstant(String value) {
        if (value == null) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO8601_DATE_TIME);
            OffsetDateTime date = OffsetDateTime.parse(value, formatter);
            return date.toInstant();
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
