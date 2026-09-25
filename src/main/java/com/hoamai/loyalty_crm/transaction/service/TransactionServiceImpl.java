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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        String transactionCode = request.getTransactionCode();
        if (transactionCode != null && !transactionCode.isBlank()) {
            // Idempotency check: Return existing transaction if duplicate transactionCode
            // is sent
            Optional<Transaction> existingTx = transactionRepository.findByTransactionCode(transactionCode);
            if (existingTx.isPresent()) {
                TransactionResponse response = mapToResponse(existingTx.get());
                response.setIsDuplicateRequest(true);
                return response;
            }
        } else {
            transactionCode = generateUniqueTransactionCode();
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + request.getStoreId()));

        TransactionStatus status = request.getStatus() != null ? request.getStatus() : TransactionStatus.SUCCESS;
        LocalDateTime transactionDate = request.getTransactionDate() != null ? request.getTransactionDate()
                : LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .transactionCode(transactionCode)
                .customer(customer)
                .store(store)
                .transactionDate(transactionDate)
                .status(status)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal grossAmount = BigDecimal.ZERO;
        for (CreateTransactionItemRequest itemRequest : request.getItems()) {
            BigDecimal subtotal = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            String productCode = itemRequest.getProductCode();
            if (productCode == null || productCode.isBlank()) {
                productCode = "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }

            TransactionItem item = TransactionItem.builder()
                    .productCode(productCode)
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .subtotal(subtotal)
                    .build();

            transaction.addItem(item);
            grossAmount = grossAmount.add(subtotal);
        }

        // Process point redemption if requested
        Long pointsToRedeem = request.getPointsToRedeem();
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (pointsToRedeem != null && pointsToRedeem > 0) {
            if (customer == null) {
                throw new IllegalArgumentException("Cannot redeem points for guest checkout");
            }

            // 1 point = 100 VND discount
            BigDecimal requestedDiscount = BigDecimal.valueOf(pointsToRedeem * 100);
            discountAmount = requestedDiscount.min(grossAmount);

            loyaltyService.redeemPointsForTransaction(customer, transaction, pointsToRedeem);
        }

        BigDecimal netTotalAmount = grossAmount.subtract(discountAmount);
        transaction.setDiscountAmount(discountAmount);
        transaction.setPointsRedeemed(pointsToRedeem != null ? pointsToRedeem : 0L);
        transaction.setTotalAmount(netTotalAmount);

        Transaction savedTransaction = transactionRepository.save(transaction);

        Long pointsEarned = 0L;
        if (status == TransactionStatus.SUCCESS && customer != null) {
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

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "transactionDate"));

        return transactionRepository.findAll(spec, pageableWithSort).map(this::mapToResponse);
    }

    private String generateUniqueTransactionCode() {
        String code;
        do {
            code = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (transactionRepository.existsByTransactionCode(code));
        return code;
    }

    private TransactionResponse mapToResponse(Transaction t) {
        BigDecimal discount = t.getDiscountAmount() != null ? t.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal netTotal = t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = netTotal.add(discount);

        return TransactionResponse.builder()
                .id(t.getId())
                .transactionCode(t.getTransactionCode())
                .customerId(t.getCustomer() != null ? t.getCustomer().getId() : null)
                .customerCode(t.getCustomer() != null ? t.getCustomer().getCustomerCode() : null)
                .customerName(t.getCustomer() != null ? t.getCustomer().getFullName() : "Khách lẻ")
                .storeId(t.getStore().getId())
                .storeCode(t.getStore().getStoreCode())
                .storeName(t.getStore().getStoreName())
                .transactionDate(t.getTransactionDate())
                .subtotalAmount(subtotal)
                .discountAmount(discount)
                .pointsRedeemed(t.getPointsRedeemed() != null ? t.getPointsRedeemed() : 0L)
                .totalAmount(netTotal)
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
