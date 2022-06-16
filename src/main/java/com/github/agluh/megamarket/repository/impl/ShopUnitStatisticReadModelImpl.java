package com.github.agluh.megamarket.repository.impl;

import com.github.agluh.megamarket.dto.ShopUnitStatistic;
import com.github.agluh.megamarket.dto.ShopUnitType;
import com.github.agluh.megamarket.repository.ShopUnitStatisticReadModel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DB based implementation of read model for statistics of catalog item.
 */
@Component
@AllArgsConstructor
public class ShopUnitStatisticReadModelImpl implements ShopUnitStatisticReadModel {

    public static final String SELECT_NODES = """
        SELECT element_id, parent_id, element_name, price, last_update, element_type
        FROM common_statistics
        WHERE element_id = :node_id
        """;
    public static final String WHERE_DATE_AFTER_OR_EQUALS = " AND last_update >= :from";
    public static final String WHERE_DATE_BEFORE = " AND last_update < :to";

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @Override
    public Collection<ShopUnitStatistic> getNodeStatistics(UUID nodeId, Instant fromIncluding,
            Instant toExcluding) {
        if (fromIncluding != null && toExcluding != null && fromIncluding.isAfter(toExcluding)) {
            throw new IllegalArgumentException("Incorrect range");
        }

        String sql = SELECT_NODES;
        Map<String, Object> params = new HashMap<>();
        params.put("node_id", nodeId);

        if (fromIncluding != null) {
            sql += WHERE_DATE_AFTER_OR_EQUALS;
            params.put("from", Timestamp.from(fromIncluding));
        }

        if (toExcluding != null) {
            sql += WHERE_DATE_BEFORE;
            params.put("to", Timestamp.from(toExcluding));
        }

        return namedJdbcTemplate.query(sql, params, this::mapRowToObject);
    }

    private ShopUnitStatistic mapRowToObject(ResultSet rs, int rowNum) throws SQLException {
        return new ShopUnitStatistic(
            rs.getObject("element_id", UUID.class),
            rs.getObject("parent_id", UUID.class),
            rs.getString("element_name"),
            rs.getObject("price", Long.class),
            rs.getTimestamp("last_update").toInstant(),
            ShopUnitType.valueOf(rs.getString("element_type"))
        );
    }
}
