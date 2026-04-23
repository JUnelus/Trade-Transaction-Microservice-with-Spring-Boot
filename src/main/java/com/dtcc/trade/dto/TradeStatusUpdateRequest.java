package com.dtcc.trade.dto;

import com.dtcc.trade.model.TradeStatus;
import jakarta.validation.constraints.NotNull;

public record TradeStatusUpdateRequest(
        @NotNull(message = "status is required")
        TradeStatus status
) {
}

