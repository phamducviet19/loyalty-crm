package com.hoamai.loyalty_crm.transaction.service;

import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionRequest;
import com.hoamai.loyalty_crm.transaction.dto.TransactionResponse;
import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse createTransaction(CreateTransactionRequest request);

    TransactionResponse getTransactionById(UUID id);

    Page<TransactionResponse> getTransactions(
            UUID customerId,
            UUID storeId,
            TransactionStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);
}
