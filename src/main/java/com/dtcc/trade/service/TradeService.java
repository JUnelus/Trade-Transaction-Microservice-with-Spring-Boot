package com.dtcc.trade.service;

import com.dtcc.trade.dto.TradeRequest;
import com.dtcc.trade.dto.TradeResponse;
import com.dtcc.trade.dto.TradeStatusUpdateRequest;
import com.dtcc.trade.exception.TradeNotFoundException;
import com.dtcc.trade.model.Trade;
import com.dtcc.trade.model.TradeStatus;
import com.dtcc.trade.repository.TradeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Page<TradeResponse> getTrades(String symbol, TradeStatus status, Pageable pageable) {
        Specification<Trade> specification = Specification.where(null);

        if (symbol != null && !symbol.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.upper(root.get("symbol")),
                            "%" + symbol.trim().toUpperCase(Locale.ROOT) + "%"
                    ));
        }

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }

        return tradeRepository.findAll(specification, pageable).map(this::toResponse);
    }

    public TradeResponse getTradeById(Long id) {
        return toResponse(getTradeEntity(id));
    }

    public TradeResponse createTrade(TradeRequest request) {
        Trade trade = new Trade();
        trade.setSymbol(normalizeSymbol(request.symbol()));
        trade.setSide(request.side());
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setStatus(TradeStatus.NEW);

        return toResponse(tradeRepository.save(trade));
    }

    public TradeResponse updateTradeStatus(Long id, TradeStatusUpdateRequest request) {
        Trade trade = getTradeEntity(id);
        validateStatusTransition(trade.getStatus(), request.status());
        trade.setStatus(request.status());
        return toResponse(tradeRepository.save(trade));
    }

    public void cancelTrade(Long id) {
        Trade trade = getTradeEntity(id);

        if (trade.getStatus() == TradeStatus.CANCELLED) {
            return;
        }

        if (trade.getStatus() == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Executed trades cannot be cancelled");
        }

        trade.setStatus(TradeStatus.CANCELLED);
        tradeRepository.save(trade);
    }

    private Trade getTradeEntity(Long id) {
        return tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException(id));
    }

    private void validateStatusTransition(TradeStatus currentStatus, TradeStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == TradeStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled trades cannot change status");
        }

        if (currentStatus == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Executed trades cannot change status");
        }
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private TradeResponse toResponse(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getSymbol(),
                trade.getSide(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getStatus(),
                trade.getCreatedAt(),
                trade.getUpdatedAt()
        );
    }
}
