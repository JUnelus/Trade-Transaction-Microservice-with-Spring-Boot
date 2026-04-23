package com.dtcc.trade.controller;

import com.dtcc.trade.dto.TradeRequest;
import com.dtcc.trade.dto.TradeResponse;
import com.dtcc.trade.dto.TradeStatusUpdateRequest;
import com.dtcc.trade.model.TradeStatus;
import com.dtcc.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    @Operation(summary = "List trades with optional pagination and filtering")
    public Page<TradeResponse> getAllTrades(@RequestParam(required = false) String symbol,
                                            @RequestParam(required = false) TradeStatus status,
                                            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return tradeService.getTrades(symbol, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single trade by id")
    public TradeResponse getTradeById(@PathVariable Long id) {
        return tradeService.getTradeById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new trade")
    public ResponseEntity<TradeResponse> createTrade(@Valid @RequestBody TradeRequest tradeRequest) {
        TradeResponse createdTrade = tradeService.createTrade(tradeRequest);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTrade.id())
                .toUri();
        return ResponseEntity.created(location).body(createdTrade);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update a trade status")
    public TradeResponse updateTradeStatus(@PathVariable Long id,
                                           @Valid @RequestBody TradeStatusUpdateRequest request) {
        return tradeService.updateTradeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a trade while preserving its audit history")
    public ResponseEntity<Void> deleteTrade(@PathVariable Long id) {
        tradeService.cancelTrade(id);
        return ResponseEntity.noContent().build();
    }
}
