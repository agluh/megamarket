package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.model.Category;
import com.github.agluh.megamarket.repository.CategoryRepository;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DB based implementation of category's repository.
 */
@Component
@AllArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    public static final String INSERT_CATEGORIES = """
        INSERT INTO categories (category_id, parent_id, prev_parent_id, category_name, last_update)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (category_id) DO UPDATE
        SET parent_id = EXCLUDED.parent_id,
            prev_parent_id = categories.parent_id, category_name = EXCLUDED.category_name,
            last_update = EXCLUDED.last_update
        """;
    public static final String DELETE_CATEGORY =
        "DELETE FROM categories WHERE category_id = ?";
    public static final String UPDATE_NODES_PRICE = """
        UPDATE categories
        SET price = (
            /* Here we describe a table contains all offers from each iterated category */
            WITH RECURSIVE sub_offers AS (
                SELECT category_id, parent_id
                FROM categories
                WHERE category_id = affected_categories.category_id
                UNION
                    SELECT categories.category_id, categories.parent_id
                    FROM categories
                    INNER JOIN sub_offers ON sub_offers.category_id = categories.parent_id
            )
            
            /* And here we calculate new price of category */
            SELECT FLOOR(AVG(price))
            FROM sub_offers
            LEFT JOIN offers ON sub_offers.category_id = offers.category_id
        )
        FROM (
            /* Here we describe a table of all parents of passed category */
            WITH RECURSIVE parents AS (
                SELECT category_id, parent_id
                FROM categories
                WHERE category_id = :node_id -- Start recursion here
                UNION
                    SELECT categories.category_id, categories.parent_id
                    FROM categories
                    INNER JOIN parents ON parents.parent_id = categories.category_id
            )
            SELECT * FROM parents
        ) AS affected_categories
        WHERE categories.category_id = affected_categories.category_id
        """;
    public static final String UPDATE_NODES_PRICE_AND_DATE = """
        UPDATE categories
        SET last_update = :date, price = (
            /* Here we describe a table contains all offers from each iterated category */
            WITH RECURSIVE sub_offers AS (
                SELECT category_id, parent_id
                FROM categories
                WHERE category_id = affected_categories.category_id
                UNION
                    SELECT categories.category_id, categories.parent_id
                    FROM categories
                    INNER JOIN sub_offers ON sub_offers.category_id = categories.parent_id
            )
            
            /* And here we calculate new price of category */
            SELECT FLOOR(AVG(price))
            FROM sub_offers
            LEFT JOIN offers ON sub_offers.category_id = offers.category_id
        )
        FROM (
            /* Here we describe a table of all parents of passed category */
            WITH RECURSIVE parents AS (
                SELECT category_id, parent_id
                FROM categories
                WHERE category_id IN (:ids) -- Start recursion here
                UNION
                    SELECT categories.category_id, categories.parent_id
                    FROM categories
                    INNER JOIN parents ON parents.parent_id = categories.category_id
            )
            SELECT * FROM parents
        ) AS affected_categories
        WHERE categories.category_id = affected_categories.category_id
        """;
    public static final String UPDATE_PARENTS_OF_MOVED_NODES = """
        UPDATE categories
        SET last_update = :date, price = (
            /* Here we describe a table contains all offers from each iterated category */
            WITH RECURSIVE sub_offers AS (
                SELECT category_id, parent_id
                FROM categories
                WHERE category_id = affected_categories.category_id
                UNION
                    SELECT categories.category_id, categories.parent_id
                    FROM categories
                    INNER JOIN sub_offers ON sub_offers.category_id = categories.parent_id
            )
            
            /* And here we calculate new price of category */
            SELECT FLOOR(AVG(price))
            FROM sub_offers
            LEFT JOIN offers ON sub_offers.category_id = offers.category_id
        )
        FROM (
            /* Here we describe table with all parents of affected items */
            WITH RECURSIVE parents AS (
                SELECT category_id, parent_id
                FROM categories
                WHERE category_id IN ( -- Start recursion here
                
                    /* Here we select all elements that was moved to other locations */
                    SELECT prev_category_id AS category_id FROM offers
                    WHERE prev_category_id <> category_id
                        OR (prev_category_id IS NULL AND category_id IS NOT NULL)
                        OR (prev_category_id IS NOT NULL AND category_id IS NULL)
                    UNION
                        SELECT prev_parent_id AS category_id FROM categories
                        WHERE (prev_parent_id <> parent_id
                            OR (prev_parent_id IS NULL AND parent_id IS NOT NULL)
                            OR (prev_parent_id IS NOT NULL AND parent_id IS NULL))
                            AND price IS NOT NULL  -- Here we ignore moving of empty categories
                )
                UNION
                    SELECT categories.category_id, categories.parent_id
                    FROM categories
                    INNER JOIN parents ON parents.parent_id = categories.category_id
            )
            SELECT * FROM parents
        ) AS affected_categories
        WHERE categories.category_id = affected_categories.category_id
        """;
    public static final String UPDATE_MOVED_OFFERS = """
        UPDATE offers SET prev_category_id = category_id
        WHERE prev_category_id <> category_id
            OR (prev_category_id IS NULL AND category_id IS NOT NULL)
            OR (prev_category_id IS NOT NULL AND category_id IS NULL)
        """;
    public static final String UPDATE_MOVED_CATEGORIES = """
        UPDATE categories SET prev_parent_id = parent_id
        WHERE prev_parent_id <> parent_id
            OR (prev_parent_id IS NULL AND parent_id IS NOT NULL)
            OR (prev_parent_id IS NOT NULL AND parent_id IS NULL)
        """;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Collection<Category> categories) {
        List<Category> list = categories.stream().toList();
        try {
            jdbcTemplate.batchUpdate(INSERT_CATEGORIES, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Category category = list.get(i);
                    ps.setObject(1, category.getId());
                    ps.setObject(2, category.getParentId());
                    ps.setObject(3, category.getParentId());
                    ps.setString(4, category.getName());
                    ps.setTimestamp(5, Timestamp.from(category.getDate()));
                }

                @Override
                public int getBatchSize() {
                    return list.size();
                }
            });
        } catch (DataIntegrityViolationException e) {
            throw new InvalidIdentityException(e);
        }
    }

    @Override
    public void delete(UUID categoryId) {
        jdbcTemplate.update(DELETE_CATEGORY, categoryId);
    }

    @Override
    public void updateUpstreamCategoriesPricesStartingWith(UUID categoryId) {
        namedJdbcTemplate.update(UPDATE_NODES_PRICE,
            Map.of("node_id", categoryId));
    }

    @Override
    public void updateCategoriesPricesAndDates(Collection<UUID> categoryIds, Instant updateDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("date", Timestamp.from(updateDate));
        params.put("ids", categoryIds);
        namedJdbcTemplate.update(UPDATE_NODES_PRICE_AND_DATE, params);
    }

    @Override
    public void updatePricesAndDatesOfOrphanedCategories(Instant updateDate) {
        namedJdbcTemplate.update(UPDATE_PARENTS_OF_MOVED_NODES,
            Map.of("date", Timestamp.from(updateDate)));

        jdbcTemplate.update(UPDATE_MOVED_OFFERS);

        jdbcTemplate.update(UPDATE_MOVED_CATEGORIES);
    }
}
