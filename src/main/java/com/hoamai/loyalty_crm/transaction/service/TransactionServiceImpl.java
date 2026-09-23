package com.hoamai.loyalty_crm.transaction.service;

import com.hoamai.loyalty_crm.common.exception.ResourceNotFoundException;
import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import com.hoamai.loyalty_crm.loyalty.service.LoyaltyService;
import com.hoamai.loyalty_crm.store.entity.Store;
import com.hoamai.loyalty_crm.store.repository.StoreRepository;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionItemRequest;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionRequest;
import com.hoamai.loyalty_crm.transaction.dto.TransactionItemResponse;
import com.hoamai.loyalty_crm.transaction.dto.TransactionResponse;
import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import com.hoamai.loyalty_crm.transaction.entity.TransactionItem;
import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import com.hoamai.loyalty_crm.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final LoyaltyService loyaltyService;

    @Override
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        // Idempotency check: Return existing transaction if duplicate transactionCode is sent
        Optional<Transaction> existingTx = transactionRepository.findByTransactionCode(request.getTransactionCode());
        if (existingTx.isPresent()) {
            TransactionResponse response = mapToResponse(existingTx.get());
            response.setIsDuplicateRequest(true);
            return response;
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + request.getStoreId()));

        TransactionStatus status = request.getStatus() != null ? request.getStatus() : TransactionStatus.SUCCESS;
        LocalDateTime transactionDate = request.getTransactionDate() != null ? request.getTransactionDate() : LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .transactionCode(request.getTransactionCode())
                .customer(customer)
                .store(store)
                .transactionDate(transactionDate)
                .status(status)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateTransactionItemRequest itemRequest : request.getItems()) {
            BigDecimal subtotal = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            TransactionItem item = TransactionItem.builder()
                    .productCode(itemRequest.getProductCode())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .subtotal(subtotal)
                    .build();

            transaction.addItem(item);
            totalAmount = totalAmount.add(subtotal);
        }

        transaction.setTotalAmount(totalAmount);
        Transaction savedTransaction = transactionRepository.save(transaction);

        Long pointsEarned = 0L;
        if (status == TransactionStatus.SUCCESS) {
            PointTransaction pointTx = loyaltyService.earnPointsForTransaction(savedTransaction);
            if (pointTx != null) {
                pointsEarned = pointTx.getPoints();
            }
        }

        TransactionResponse response = mapToResponse(savedTransaction);
        response.setPointsEarned(pointsEarned);
        response.setIsDuplicateRequest(false);
        return response;
    }

    @Override
    public TransactionResponse getTransactionById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getTransactions(
            UUID customerId,
            UUID storeId,
            TransactionStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        return transactionRepository.findTransactions(customerId, storeId, status, fromDate, toDate, pageable)
                .map(this::mapToResponse);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .transactionCode(t.getTransactionCode())
                .customerId(t.getCustomer().getId())
                .customerCode(t.getCustomer().getCustomerCode())
                .customerName(t.getCustomer().getFullName())
                .storeId(t.getStore().getId())
                .storeCode(t.getStore().getStoreCode())
                .storeName(t.getStore().getStoreName())
                .transactionDate(t.getTransactionDate())
                .totalAmount(t.getTotalAmount())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .items(t.getItems().stream().map(item -> TransactionItemResponse.builder()
                        .id(item.getId())
                        .productCode(item.getProductCode())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
