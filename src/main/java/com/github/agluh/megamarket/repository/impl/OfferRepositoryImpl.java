package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.model.Offer;
import com.github.agluh.megamarket.repository.OfferRepository;
import com.github.agluh.megamarket.repository.exception.InvalidIdentityException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
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
        INSERT INTO offers (offer_id, category_id, prev_category_id, offer_name, price, last_update)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (offer_id) DO UPDATE
        SET category_id = EXCLUDED.category_id,
            prev_category_id = offers.category_id, offer_name = EXCLUDED.offer_name,
            price = EXCLUDED.price, last_update = EXCLUDED.last_update
        """;

    public static final String DELETE_OFFER =
        "DELETE FROM offers WHERE offer_id = ?";

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
                    ps.setObject(3, offer.getParentId());
                    ps.setString(4, offer.getName());
                    ps.setLong(5, offer.getPrice());
                    ps.setTimestamp(6, Timestamp.from(offer.getDate()));
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
}
