package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.dto.ShopUnit;
import com.github.agluh.megamarket.dto.ShopUnitType;
import com.github.agluh.megamarket.repository.ShopUnitReadModel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DB based implementation of read model for catalog item.
 */
@Component
@AllArgsConstructor
public class ShopUnitReadModelImpl implements ShopUnitReadModel {

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
            
            /* ...and finally we're trying to find a single offer
            if none categories found by such ID */
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
    public Collection<ShopUnit> getNodeWithSubtree(UUID nodeId) {
        return namedJdbcTemplate.query(
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
