package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.model.Category;
import com.github.agluh.megamarket.repository.CategoryRepository;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
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

    private static final String INSERT_CATEGORIES = """
        INSERT INTO categories (category_id, parent_id, category_name, price, last_update)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (category_id) DO UPDATE
        SET parent_id = EXCLUDED.parent_id,
            category_name = EXCLUDED.category_name,
            price = EXCLUDED.price,
            last_update = EXCLUDED.last_update
        """;
    private static final String DELETE_CATEGORY =
        "DELETE FROM categories WHERE category_id = ?";
    private static final String SELECT_CATEGORIES = """
        SELECT category_id, parent_id, category_name, price, last_update
        FROM categories
        WHERE category_id IN (:ids)
        """;
    private static final String SELECT_PARENTS = """
        WITH RECURSIVE parents AS (
            SELECT category_id, parent_id, category_name, price, last_update
            FROM categories
            WHERE category_id IN (:ids) -- Start recursion here
            UNION
                SELECT c.category_id, c.parent_id, c.category_name, c.price, c.last_update
                FROM categories c
                INNER JOIN parents ON parents.parent_id = c.category_id
        )
        SELECT category_id, parent_id, category_name, price, last_update
        FROM parents
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
                    ps.setString(3, category.getName());
                    ps.setObject(4, category.getPrice());
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
    public Collection<Category> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        return namedJdbcTemplate.query(SELECT_CATEGORIES, Map.of("ids", ids), this::mapRowToObject);
    }

    @Override
    public Collection<Category> getAllUpstreamCategories(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        return namedJdbcTemplate.query(SELECT_PARENTS, Map.of("ids", ids), this::mapRowToObject);
    }

    private Category mapRowToObject(ResultSet rs, int rowNum) throws SQLException {
        return new Category(
            rs.getObject("category_id", UUID.class),
            rs.getObject("parent_id", UUID.class),
            rs.getString("category_name"),
            rs.getObject("price", Long.class),
            rs.getTimestamp("last_update").toInstant()
        );
    }
}
