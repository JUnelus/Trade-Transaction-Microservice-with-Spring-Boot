package com.dtcc.trade.service;

import com.dtcc.trade.dto.TradeRequest;
import com.dtcc.trade.dto.TradeStatusUpdateRequest;
import com.dtcc.trade.model.Trade;
import com.dtcc.trade.model.TradeSide;
import com.dtcc.trade.model.TradeStatus;
import com.dtcc.trade.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void createTradeNormalizesSymbolAndDefaultsStatusToNew() {
        TradeRequest request = new TradeRequest(" aapl ", TradeSide.BUY, 25, new BigDecimal("100.50"));
        Trade savedTrade = new Trade("AAPL", TradeSide.BUY, 25, new BigDecimal("100.50"), TradeStatus.NEW);
        savedTrade.setId(1L);
        savedTrade.setCreatedAt(OffsetDateTime.parse("2026-04-22T00:00:00Z"));
        savedTrade.setUpdatedAt(OffsetDateTime.parse("2026-04-22T00:00:00Z"));

        when(tradeRepository.save(any(Trade.class))).thenReturn(savedTrade);

        var response = tradeService.createTrade(request);
        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());

        assertThat(tradeCaptor.getValue().getSymbol()).isEqualTo("AAPL");
        assertThat(tradeCaptor.getValue().getStatus()).isEqualTo(TradeStatus.NEW);
        assertThat(response.symbol()).isEqualTo("AAPL");
        assertThat(response.status()).isEqualTo(TradeStatus.NEW);
    }

    @Test
    void updateTradeStatusRejectsExecutedTrades() {
        Trade trade = new Trade("AAPL", TradeSide.BUY, 25, new BigDecimal("100.50"), TradeStatus.EXECUTED);
        trade.setId(7L);
        when(tradeRepository.findById(7L)).thenReturn(Optional.of(trade));

        assertThatThrownBy(() -> tradeService.updateTradeStatus(7L, new TradeStatusUpdateRequest(TradeStatus.CANCELLED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Executed trades cannot change status");
    }

    @Test
    void cancelTradeMarksNewTradeAsCancelled() {
        Trade trade = new Trade("MSFT", TradeSide.SELL, 10, new BigDecimal("250.00"), TradeStatus.NEW);
        trade.setId(3L);
        when(tradeRepository.findById(3L)).thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        tradeService.cancelTrade(3L);

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CANCELLED);
        verify(tradeRepository).save(trade);
    }
}

