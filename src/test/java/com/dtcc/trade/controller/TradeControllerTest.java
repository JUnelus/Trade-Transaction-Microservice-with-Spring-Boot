package com.dtcc.trade.controller;

import com.dtcc.trade.dto.TradeRequest;
import com.dtcc.trade.dto.TradeResponse;
import com.dtcc.trade.exception.TradeNotFoundException;
import com.dtcc.trade.model.TradeSide;
import com.dtcc.trade.model.TradeStatus;
import com.dtcc.trade.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TradeService tradeService;

    @Test
    void createTradeReturnsCreatedResponse() throws Exception {
        TradeRequest request = new TradeRequest("aapl", TradeSide.BUY, 100, new BigDecimal("50.25"));
        TradeResponse response = new TradeResponse(
                1L,
                "AAPL",
                TradeSide.BUY,
                100,
                new BigDecimal("50.25"),
                TradeStatus.NEW,
                OffsetDateTime.parse("2026-04-22T00:00:00Z"),
                OffsetDateTime.parse("2026-04-22T00:00:00Z")
        );

        when(tradeService.createTrade(any(TradeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/trades/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void createTradeReturnsBadRequestForInvalidPayload() throws Exception {
        TradeRequest request = new TradeRequest("", null, 0, BigDecimal.ZERO);

        mockMvc.perform(post("/api/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.symbol").exists())
                .andExpect(jsonPath("$.validationErrors.side").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists());
    }

    @Test
    void getTradeByIdReturnsNotFoundWhenMissing() throws Exception {
        when(tradeService.getTradeById(eq(99L))).thenThrow(new TradeNotFoundException(99L));

        mockMvc.perform(get("/api/trades/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Trade with id 99 was not found"));
    }
}

