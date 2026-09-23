package com.hoamai.loyalty_crm.transaction.controller;

import com.hoamai.loyalty_crm.common.dto.ApiResponse;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionRequest;
import com.hoamai.loyalty_crm.transaction.dto.TransactionResponse;
import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import com.hoamai.loyalty_crm.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Idempotent POS Transaction Creation API.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        HttpStatus httpStatus = Boolean.TRUE.equals(response.getIsDuplicateRequest()) ? HttpStatus.OK : HttpStatus.CREATED;
        String message = Boolean.TRUE.equals(response.getIsDuplicateRequest())
                ? "Duplicate transaction code detected. Returning existing transaction."
                : "Transaction created successfully";

        return ResponseEntity.status(httpStatus).body(ApiResponse.success(message, response));
    }

    /**
     * Transaction Details API.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable("id") UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Transaction History & Search API.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "storeId", required = false) UUID storeId,
            @RequestParam(name = "status", required = false) TransactionStatus status,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Page<TransactionResponse> response = transactionService.getTransactions(
                customerId, storeId, status, fromDate, toDate, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
