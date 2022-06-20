package com.github.agluh.megamarket.repository;

import com.github.agluh.megamarket.model.Category;
import java.util.Collection;
import java.util.UUID;

/**
 * Repository for categories.
 */
public interface CategoryRepository {

    void save(Collection<Category> categories);

    void delete(UUID categoryId);

    Collection<Category> findByIds(Collection<UUID> ids);

    Collection<Category> getAllUpstreamCategories(Collection<UUID> ids);
}
