package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.model.Offer;
import com.github.agluh.megamarket.repository.OfferRepository;
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
 * DB based implementation of offer's repository.
 */
@Component
@AllArgsConstructor
public class OfferRepositoryImpl implements OfferRepository {

    public static final String INSERT_OFFERS = """
        INSERT INTO offers (offer_id, category_id, offer_name, price, last_update)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (offer_id) DO UPDATE
        SET category_id = EXCLUDED.category_id,
            offer_name = EXCLUDED.offer_name,
            price = EXCLUDED.price,
            last_update = EXCLUDED.last_update
        """;
    public static final String DELETE_OFFER =
        "DELETE FROM offers WHERE offer_id = ?";
    public static final String SELECT_OFFERS = """
        SELECT offer_id, category_id, offer_name, price, last_update
        FROM offers
        WHERE offer_id IN (:ids)
        """;
    public static final String SELECT_AVG_PRICE = """
        WITH RECURSIVE sub_offers AS (
            SELECT category_id, parent_id
            FROM categories
            WHERE category_id = :node_id -- Start recursion here
            UNION
                SELECT categories.category_id, categories.parent_id
                FROM categories
                INNER JOIN sub_offers ON sub_offers.category_id = categories.parent_id
        )
        SELECT FLOOR(AVG(price))
        FROM sub_offers
        LEFT JOIN offers ON sub_offers.category_id = offers.category_id
        """;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Collection<Offer> offers) {
        List<Offer> list = offers.stream().toList();
        try {
            jdbcTemplate.batchUpdate(INSERT_OFFERS, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Offer offer = list.get(i);
                    ps.setObject(1, offer.getId());
                    ps.setObject(2, offer.getParentId());
                    ps.setString(3, offer.getName());
                    ps.setLong(4, offer.getPrice());
                    ps.setTimestamp(5, Timestamp.from(offer.getDate()));
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
    public void delete(UUID offerId) {
        jdbcTemplate.update(DELETE_OFFER, offerId);
    }

    @Override
    public Collection<Offer> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        return namedJdbcTemplate.query(SELECT_OFFERS, Map.of("ids", ids), this::mapRowToObject);
    }

    @Override
    public Long getAvgPriceOfCategory(UUID categoryId) {
        return namedJdbcTemplate.queryForObject(SELECT_AVG_PRICE,
            Map.of("node_id", categoryId), Long.class);
    }

    private Offer mapRowToObject(ResultSet rs, int rowNum) throws SQLException {
        return new Offer(
            rs.getObject("offer_id", UUID.class),
            rs.getObject("category_id", UUID.class),
            rs.getString("offer_name"),
            rs.getLong("price"),
            rs.getTimestamp("last_update").toInstant()
        );
    }
}
