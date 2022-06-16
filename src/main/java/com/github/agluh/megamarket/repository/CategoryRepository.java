package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.Category;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Repository for categories.
 */
public interface CategoryRepository {

    void save(Collection<Category> categories);

    void delete(UUID categoryId);

    /**
     * Updates price for all upstream categories starting with {@code categoryId}.
     *
     * <p>Dates on any elements are not updated.
     */
    void updateUpstreamCategoriesPricesStartingWith(UUID categoryId);

    /**
     * Updates price and date for all upstream categories starting
     * with each node from {@code categoryIds}.
     */
    void updateCategoriesPricesAndDates(Collection<UUID> categoryIds, Instant updateDate);

    /**
     * Updates price and date in each category that has lost some of their children.
     */
    void updatePricesAndDatesOfOrphanedCategories(Instant updateDate);
}
