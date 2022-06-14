package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.model.ShopUnitType;
import com.github.agluh.megamarket.repository.ShopUnitRepository;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ShopUnitRepositoryImpl implements ShopUnitRepository {

    public static final String INSERT_CATEGORIES = """
        INSERT INTO categories (category_id, parent_id, prev_parent_id, category_name, last_update)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (category_id) DO UPDATE
        SET parent_id = EXCLUDED.parent_id,
            prev_parent_id = categories.parent_id, category_name = EXCLUDED.category_name,
            last_update = EXCLUDED.last_update
        """;
    public static final String INSERT_OFFERS = """
        INSERT INTO offers (offer_id, category_id, prev_category_id, offer_name, price, last_update)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (offer_id) DO UPDATE
        SET category_id = EXCLUDED.category_id,
            prev_category_id = offers.category_id, offer_name = EXCLUDED.offer_name,
            price = EXCLUDED.price, last_update = EXCLUDED.last_update
        """;
    public static final String DELETE_OFFER =
        "DELETE FROM offers WHERE offer_id = ?";
    public static final String DELETE_CATEGORY =
        "DELETE FROM categories WHERE category_id = ?";
    public static final String SELECT_NODE = """
        /* Here we're trying to find category by passed ID */
        SELECT
            category_id AS element_id,
            parent_id,
            category_name AS element_name,
            'CATEGORY' as element_type,
            price,
            last_update
        FROM categories
        WHERE category_id = :node_id
        
        /* And then we're trying find an offer by the same ID */
        UNION
            SELECT
                offer_id AS element_id,
                category_id AS parent_id,
                offer_name AS element_name,
                'OFFER' as element_type,
                price,
                last_update
            FROM offers
            WHERE offer_id = :node_id
        """;
    public static final String SELECT_NODE_SUBTREE = """
        /* Here we just describe a table of all subcategories of category with passed ID */
        WITH RECURSIVE tree AS (
            SELECT category_id, parent_id, category_name, price, last_update
            FROM categories
            WHERE category_id = :node_id
            UNION
                SELECT c.category_id, c.parent_id, c.category_name, c.price, c.last_update
                FROM categories c
                INNER JOIN tree t ON t.category_id = c.parent_id
        )
        
        /* Here we directly select a categories from that table... */
        SELECT
            category_id AS element_id,
            parent_id AS parent_id,
            category_name AS element_name,
            'CATEGORY' AS element_type,
            price,
            last_update
        FROM tree
        
        /* ...here we join any offers from them too */
        UNION
            SELECT
                offer_id AS element_id,
                o.category_id AS parent_id,
                offer_name AS element_name,
                'OFFER' AS element_type,
                o.price,
                o.last_update
            FROM tree
            LEFT JOIN offers o ON tree.category_id = o.category_id
            WHERE o.category_id IS NOT null
            
            /* ...and finally we're trying to find a single offer if none categories found by such ID */
            UNION
                SELECT
                    offer_id AS element_id,
                    category_id AS parent_id,
                    offer_name AS element_name,
                    'OFFER' AS element_type,
                    o.price,
                    last_update
                FROM offers o
                WHERE offer_id = :node_id
        """;
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
                
                    /* Here we select all element that was moved to other location */
                    SELECT prev_category_id AS category_id FROM offers
                    WHERE prev_category_id <> category_id
                    UNION
                        SELECT prev_parent_id AS category_id FROM categories
                        WHERE prev_parent_id <> parent_id
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
        """;
    public static final String UPDATE_MOVED_CATEGORIES = """
        UPDATE categories SET prev_parent_id = parent_id
        WHERE prev_parent_id <> parent_id
        """;
    public static final String SELECT_OFFERS_UPDATED_BETWEEN = """
        SELECT
            offer_id AS element_id,
            category_id AS parent_id,
            offer_name AS element_name,
            'OFFER' as element_type,
            price,
            last_update
        FROM offers
        WHERE last_update BETWEEN ? AND ?
        """;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveCategories(List<ShopUnit> nodes) {
        try {
            jdbcTemplate.batchUpdate(INSERT_CATEGORIES, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ShopUnit shopUnit = nodes.get(i);
                    ps.setObject(1, shopUnit.getId());
                    ps.setObject(2, shopUnit.getParentId());
                    ps.setObject(3, shopUnit.getParentId());
                    ps.setString(4, shopUnit.getName());
                    ps.setTimestamp(5, Timestamp.from(shopUnit.getDate()));
                }

                @Override
                public int getBatchSize() {
                    return nodes.size();
                }
            });
        } catch (DataIntegrityViolationException e) {
            throw new InvalidIdentityException(e);
        }
    }

    @Override
    public void saveOffers(List<ShopUnit> nodes) {
        try {
            jdbcTemplate.batchUpdate(INSERT_OFFERS, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ShopUnit shopUnit = nodes.get(i);
                    ps.setObject(1, shopUnit.getId());
                    ps.setObject(2, shopUnit.getParentId());
                    ps.setObject(3, shopUnit.getParentId());
                    ps.setString(4, shopUnit.getName());
                    ps.setLong(5, shopUnit.getPrice());
                    ps.setTimestamp(6, Timestamp.from(shopUnit.getDate()));
                }

                @Override
                public int getBatchSize() {
                    return nodes.size();
                }
            });
        } catch (DataIntegrityViolationException e) {
            throw new InvalidIdentityException(e);
        }
    }

    @Override
    public Collection<ShopUnit> getNodeWithSubtree(UUID nodeId) {
        return  namedJdbcTemplate.query(
            SELECT_NODE_SUBTREE,
            Map.of("node_id", nodeId),
            this::mapRowToObject
        );
    }

    @Override
    public Collection<ShopUnit> getOffersUpdatedBetween(Instant fromIncluding,
            Instant toIncluding) {
        if (fromIncluding.isAfter(toIncluding)) {
            throw new IllegalArgumentException("Incorrect range");
        }

        return jdbcTemplate.query(SELECT_OFFERS_UPDATED_BETWEEN, this::mapRowToObject,
            Timestamp.from(fromIncluding), Timestamp.from(toIncluding));
    }

    @Override
    public Optional<ShopUnit> getNode(UUID nodeId) {
        return namedJdbcTemplate.query(
            SELECT_NODE,
            Map.of("node_id", nodeId),
            this::mapRowToObject
        ).stream().findAny();
    }

    @Override
    public void deleteNode(ShopUnit node) {
        jdbcTemplate.update(node.isCategory() ? DELETE_CATEGORY : DELETE_OFFER,
            node.getId());
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

    private ShopUnit mapRowToObject(ResultSet rs, int rowNum) throws SQLException {
        return new ShopUnit(
            rs.getObject("element_id", UUID.class),
            rs.getObject("parent_id", UUID.class),
            rs.getString("element_name"),
            rs.getObject("price", Long.class),
            rs.getTimestamp("last_update").toInstant(),
            ShopUnitType.valueOf(rs.getString("element_type"))
        );
    }
}
