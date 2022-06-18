package com.github.agluh.megamarket.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import com.github.agluh.megamarket.dto.ShopUnit;
import com.github.agluh.megamarket.dto.ShopUnitImport;
import com.github.agluh.megamarket.dto.ShopUnitStatistic;
import com.github.agluh.megamarket.dto.ShopUnitType;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import com.github.agluh.megamarket.service.exceptions.IdentityIsNotUniqueException;
import com.github.agluh.megamarket.service.exceptions.ShopUnitNotFoundException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.jdbc.JdbcTestUtils;

@SpringBootTest
class ShopServiceIntegrationTest {

    @Autowired
    private ShopService shopService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "categories", "offers");
    }

    @Test
    void givenNonExistingOffer_whenImportOffer_thenNewOfferShouldBeAdded() {
        // Given: empty DB
        final ShopUnitImport offer = createOffer(null, "Offer", 100L);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(offer), date);

        // Then
        ShopUnit node = shopService.getNode(offer.getId());
        then(node).extracting("id").isEqualTo(offer.getId());
        then(node).extracting("name").isEqualTo("Offer");
        then(node).extracting("price").isEqualTo(100L);
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(date);
    }

    @Test
    void givenNonExistingCategory_whenImportCategory_thenNewCategoryShouldBeAdded() {
        // Given: empty DB
        final ShopUnitImport category = createCategory(null, "Category");
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(category), date);

        // Then
        ShopUnit node = shopService.getNode(category.getId());
        then(node).extracting("id").isEqualTo(category.getId());
        then(node).extracting("name").isEqualTo("Category");
        then(node).extracting("price").isNull();
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(date);
    }

    @Test
    @Sql({"/import_single_offer.sql"})
    void givenExistingOffer_whenGetOffer_thenShouldGetCorrectOffer() {
        // Given:
        final UUID offerId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");

        // When
        ShopUnit node = shopService.getNode(offerId);

        // Then
        then(node).extracting("id").isEqualTo(offerId);
        then(node).extracting("name").isEqualTo("Offer");
        then(node).extracting("price").isEqualTo(100L);
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(Instant.parse("2022-05-28T21:12:01.000Z"));
    }

    @Test
    @Sql({"/import_single_category.sql"})
    void givenExistingCategory_whenGetCategory_thenShouldGetCorrectCategory() {
        // Given:
        final UUID categoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");

        // When
        ShopUnit node = shopService.getNode(categoryId);

        // Then
        then(node).extracting("id").isEqualTo(categoryId);
        then(node).extracting("name").isEqualTo("Category");
        then(node).extracting("price").isNull();
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(Instant.parse("2022-05-28T21:12:01.000Z"));
    }

    @Test
    @Sql({"/import_single_offer.sql"})
    void givenExistingOffer_whenImportOfferWithSameId_thenExistedOfferShouldBeUpdated() {
        // Given
        final ShopUnitImport offer = createOffer(null, "Updated", 200L);
        offer.setId(UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002"));
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(offer), date);

        // Then
        ShopUnit node = shopService.getNode(offer.getId());
        then(node).extracting("id").isEqualTo(offer.getId());
        then(node).extracting("name").isEqualTo("Updated");
        then(node).extracting("price").isEqualTo(200L);
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(date);
    }

    @Test
    @Sql({"/import_single_category.sql"})
    void givenExistingCategory_whenImportCategoryWithSameId_thenExistedCategoryShouldBeUpdated() {
        // Given
        final ShopUnitImport category = createCategory(null, "Updated");
        category.setId(UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002"));
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(category), date);

        // Then
        ShopUnit node = shopService.getNode(category.getId());
        then(node).extracting("id").isEqualTo(category.getId());
        then(node).extracting("name").isEqualTo("Updated");
        then(node).extracting("price").isNull();
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(date);
    }

    @Test
    @Sql({"/import_category_with_single_offer.sql"})
    void givenExistingCategory_whenImportNewOfferAtThisCategory_thenCategoryPriceAndDateShouldBeUpdated() {
        // Given
        final UUID categoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport offer = createOffer(categoryId, "Offer 2", 80L);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(offer), date);

        // Then
        ShopUnit node = shopService.getNode(categoryId);
        then(node).extracting("id").isEqualTo(categoryId);
        then(node).extracting("name").isEqualTo("Category");
        then(node).extracting("price").isEqualTo(50L);
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(date);
    }

    @Test
    @Sql({"/import_tree_of_categories.sql"})
    void givenExistingCategoriesSubtree_whenImportNewOfferAsLeafOfSubtree_thenUpstreamParentCategoriesPriceAndDateShouldBeUpdated() {
        // Given
        final UUID rootCategoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");
        final UUID categoryId = UUID.fromString("915db3ea-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport offer = createOffer(categoryId, "Offer", 100L);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(offer), date);

        // Then
        ShopUnit node = shopService.getNode(rootCategoryId);
        then(node).extracting("id").isEqualTo(rootCategoryId);
        then(node).extracting("name").isEqualTo("Root category");
        then(node).extracting("price").isEqualTo(100L);
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(date);

        Collection<ShopUnit> subTree = node.getChildren();
        then(subTree).element(0).extracting("id").isEqualTo(categoryId);
        then(subTree).element(0).extracting("name").isEqualTo("Sub cat 1");
        then(subTree).element(0).extracting("price").isEqualTo(100L);
        then(subTree).element(0).extracting("parentId").isEqualTo(rootCategoryId);
        then(subTree).element(0).extracting("date").isEqualTo(date);
    }

    @Test
    @Sql({"/import_single_category.sql"})
    void givenExistedCategory_whenImportNewCategoryAtThisCategory_thenParentCategoryPriceAndDateShouldNotBeUpdated() {
        // Given
        final UUID categoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport category = createCategory(categoryId, "Category 2");
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(category), date);

        // Then
        ShopUnit node = shopService.getNode(categoryId);
        then(node).extracting("id").isEqualTo(categoryId);
        then(node).extracting("name").isEqualTo("Category");
        then(node).extracting("price").isNull();
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(Instant.parse("2022-05-28T21:12:01.000Z"));
    }

    @Test
    @Sql({"/import_single_category.sql"})
    void givenExistedCategory_whenDeleteCategory_thenCategoryShouldBeDeleted() {
        // Given
        final UUID categoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");

        // When
        shopService.deleteNode(categoryId);

        // Then
        final Throwable throwable = catchThrowable(() -> shopService.getNode(categoryId));
        then(throwable).isInstanceOf(ShopUnitNotFoundException.class);
    }

    @Test
    @Sql({"/import_single_offer.sql"})
    void givenExistedOffer_whenDeleteOffer_thenOfferShouldBeDeleted() {
        // Given
        final UUID offerId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");

        // When
        shopService.deleteNode(offerId);

        // Then
        final Throwable throwable = catchThrowable(() -> shopService.getNode(offerId));
        then(throwable).isInstanceOf(ShopUnitNotFoundException.class);
    }

    @Test
    void givenNonExistedCategory_whenDeleteCategory_thenExceptionShouldBeThrown() {
        // Given
        final UUID categoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");

        // When
        final Throwable throwable = catchThrowable(() -> shopService.deleteNode(categoryId));

        // Then
        then(throwable).isInstanceOf(ShopUnitNotFoundException.class);
    }

    @Test
    void givenNonExistedOffer_whenDeleteOffer_thenExceptionShouldBeThrown() {
        // Given
        final UUID offerId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");

        // When
        final Throwable throwable = catchThrowable(() -> shopService.deleteNode(offerId));

        // Then
        then(throwable).isInstanceOf(ShopUnitNotFoundException.class);
    }

    @Test
    @Sql({"/import_category_with_single_offer.sql"})
    void givenExistedCategoryAndOffer_whenDeleteOffer_thenCategoryPriceShouldBeUpdatedButDateShouldNot() {
        // Given
        final UUID categoryId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");
        final String lastCategoryUpdate = "2022-05-28T21:12:01.000Z";
        final UUID offerId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");

        // When
        shopService.deleteNode(offerId);

        // Then
        ShopUnit node = shopService.getNode(categoryId);
        then(node).extracting("id").isEqualTo(categoryId);
        then(node).extracting("name").isEqualTo("Category");
        then(node).extracting("price").isNull();
        then(node).extracting("parentId").isNull();
        then(node).extracting("date").isEqualTo(Instant.parse(lastCategoryUpdate));
    }

    @Test
    @Sql({"/import_two_subtree_of_categories_and_offer.sql"})
    void givenTwoSubtreeOfCategoriesWithOfferInOneOfThem_whenOfferMovedToOtherSubtree_thenBothSubtreeShouldUpdateDateAndPrice() {
        // Given
        final UUID firstRootId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");
        final UUID secondRootId = UUID.fromString("915db110-e71f-11ec-8fea-0242ac120002");
        final UUID newParentId = UUID.fromString("915db52a-e71f-11ec-8fea-0242ac120002");
        final UUID offerId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport offer = createOffer(newParentId, "Updated offer", 50L);
        offer.setId(offerId);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(offer), date);

        // Then
        ShopUnit prevOwnerSubtreeRoot = shopService.getNode(firstRootId);
        then(prevOwnerSubtreeRoot).extracting("id").isEqualTo(firstRootId);
        then(prevOwnerSubtreeRoot).extracting("price").isNull();
        then(prevOwnerSubtreeRoot).extracting("date").isEqualTo(date);

        ShopUnit newOwnerSubtreeRoot = shopService.getNode(secondRootId);
        then(newOwnerSubtreeRoot).extracting("id").isEqualTo(secondRootId);
        then(newOwnerSubtreeRoot).extracting("price").isEqualTo(50L);
        then(newOwnerSubtreeRoot).extracting("date").isEqualTo(date);
    }

    @Test
    @Sql({"/import_two_subtree_of_categories_and_offer.sql"})
    void givenTwoSubtreeOfCategoriesWithoutOffersInThem_whenCategoryMovedToOtherSubtree_thenNoParentCategoryShouldUpdateDate() {
        // Given
        final UUID categoryId = UUID.fromString("915db52a-e71f-11ec-8fea-0242ac120002");
        final UUID newParentId = UUID.fromString("915daef4-e71f-11ec-8fea-0242ac120002");
        final UUID oldParent = UUID.fromString("915db110-e71f-11ec-8fea-0242ac120002");
        ShopUnitImport category = createCategory(newParentId, "Moved category");
        category.setId(categoryId);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(category), date);

        // Then
        ShopUnit prevOwnerSubtreeRoot = shopService.getNode(oldParent);
        then(prevOwnerSubtreeRoot).extracting("id").isEqualTo(oldParent);
        then(prevOwnerSubtreeRoot).extracting("price").isNull();
        then(prevOwnerSubtreeRoot).extracting("date").isEqualTo(Instant.parse("2022-05-28T21:12:01.000Z"));

        ShopUnit newOwnerSubtreeRoot = shopService.getNode(newParentId);
        then(newOwnerSubtreeRoot).extracting("id").isEqualTo(newParentId);
        then(newOwnerSubtreeRoot).extracting("price").isEqualTo(100L);
        then(newOwnerSubtreeRoot).extracting("date").isEqualTo(Instant.parse("2022-05-28T21:12:01.000Z"));
    }

    @Test
    void givenNotExistingCategory_whenImportOfferWithSuchParentCategory_thenExceptionShouldBeThrown() {
        // Given
        final UUID notExistedParentId = UUID.fromString("915db110-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport offer = createOffer(notExistedParentId, "Offer", 50L);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.importData(List.of(offer), date));

        // Then
        then(throwable).isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void givenNotExistingCategory_whenImportCategoryWithSuchParentCategory_thenExceptionShouldBeThrown() {
        // Given
        final UUID notExistedParentId = UUID.fromString("915db110-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport category = createCategory(notExistedParentId, "Category");
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.importData(List.of(category), date));

        // Then
        then(throwable).isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    void givenTwoDependentCategories_whenImportInReversedOrder_thenShouldBeCorrectlyHandled() {
        // Given
        final ShopUnitImport rootCategory = createCategory(null, "Root");
        final ShopUnitImport dependentCategory = createCategory(rootCategory.getId(), "Category");
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.importData(List.of(dependentCategory, rootCategory), date));

        // Then
        then(throwable).isNull();
    }

    @Test
    @Sql({"/import_single_offer.sql"})
    void givenExistingOffer_whenImportOfferWithParentOfExistingOffer_thenExceptionShouldBeThrown() {
        // Given
        final UUID offerParentId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport offer = createOffer(offerParentId, "Offer", 50L);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.importData(List.of(offer), date));

        // Then
        then(throwable).isInstanceOf(InvalidIdentityException.class);
    }

    @Test
    @Sql({"/import_single_offer.sql"})
    void givenExistingOffer_whenImportCategoryWithParentOfExistingOffer_thenExceptionShouldBeThrown() {
        // Given
        final UUID offerParentId = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport category = createCategory(offerParentId, "Category");
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.importData(List.of(category), date));

        // Then
        then(throwable).isInstanceOf(InvalidIdentityException.class);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("provideDataForGetSales")
    @Sql({"/import_single_offer.sql"})
    void givenOfferDateWithinRange_whenGetSales_thenOfferShouldBeFetched(String nowDate, boolean expectedIsFetched, String message) {
        // Given
        Instant now = Instant.parse(nowDate);

        // When
        Collection<ShopUnit> list = shopService.getSales(now);

        // Then
        then(list).hasSize(expectedIsFetched ? 1 : 0);
    }

    private static Stream<Arguments> provideDataForGetSales() {
        return Stream.of(
            // 2022-05-28T21:12:01.000Z + 24h + 1s = 2022-05-29T21:12:02.000Z
            Arguments.of("2022-05-29T21:12:02.000Z", false, "before left range limit"),
            // 2022-05-28T21:12:01.000Z + 24h = 2022-05-29T21:12:01.000Z
            Arguments.of("2022-05-29T21:12:01.000Z", true, "at left range limit"),
            // 2022-05-28T21:12:01.000Z + 1h = 2022-05-28T22:12:01.000Z
            Arguments.of("2022-05-28T22:12:01.000Z", true, "within range"),
            Arguments.of("2022-05-28T21:12:01.000Z", true, "at right range limit"),
            // 2022-05-28T21:12:01.000Z - 1s = // 2022-05-28T21:12:00.000Z
            Arguments.of("2022-05-28T21:12:00.000Z", false, "after right range limit")
        );
    }

    @Test
    void givenTreeOfSubcategories_whenImport_thenCategoriesShouldBePlacedToStatisticsOnce() {
        // Given
        final ShopUnitImport rootCategory = createCategory(null, "Root");
        final ShopUnitImport subCategory = createCategory(rootCategory.getId(), "Sub category");
        final ShopUnitImport offer = createOffer(subCategory.getId(), "Offer", 100);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        shopService.importData(List.of(rootCategory, subCategory, offer), date);

        // Then
        Collection<ShopUnitStatistic> stat = shopService.getNodeStatistics(rootCategory.getId(), null, null);
        then(stat).hasSize(1);
    }

    @Test
    void givenElementsWithDuplicatedIds_whenImport_thenExceptionShouldBeThrown() {
        // Given
        final UUID id = UUID.fromString("915dbed0-e71f-11ec-8fea-0242ac120002");
        final ShopUnitImport category = createCategory(null, "Category");
        category.setId(id);
        final ShopUnitImport offer = createOffer(null, "Offer", 100);
        offer.setId(id);
        final Instant date = Instant.parse("2022-06-13T10:30:00.000Z");

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.importData(List.of(category, offer), date));

        // Then
        then(throwable).isInstanceOf(IdentityIsNotUniqueException.class);
    }

    private ShopUnitImport createOffer(UUID parentId, String name, long price) {
        return new ShopUnitImport(UUID.randomUUID(), name, ShopUnitType.OFFER, parentId, price);
    }

    private ShopUnitImport createCategory(UUID parentId, String name) {
        return new ShopUnitImport(UUID.randomUUID(), name, ShopUnitType.CATEGORY, parentId, null);
    }
}