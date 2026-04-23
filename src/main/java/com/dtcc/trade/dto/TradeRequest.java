package com.dtcc.trade.dto;

import com.dtcc.trade.model.TradeSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TradeRequest(
        @NotBlank(message = "symbol is required")
        @Size(max = 20, message = "symbol must be at most 20 characters")
        String symbol,

        @NotNull(message = "side is required")
        TradeSide side,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be greater than 0")
        Integer quantity,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "price must be greater than 0")
        BigDecimal price
) {
}

