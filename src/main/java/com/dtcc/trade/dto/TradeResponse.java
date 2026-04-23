package com.dtcc.trade.dto;

import com.dtcc.trade.model.TradeSide;
import com.dtcc.trade.model.TradeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TradeResponse(
        Long id,
        String symbol,
        TradeSide side,
        Integer quantity,
        BigDecimal price,
        TradeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

