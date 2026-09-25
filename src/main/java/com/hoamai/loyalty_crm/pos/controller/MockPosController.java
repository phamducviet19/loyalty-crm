package com.hoamai.loyalty_crm.pos.controller;

import com.hoamai.loyalty_crm.common.dto.ApiResponse;
import com.hoamai.loyalty_crm.common.exception.ResourceNotFoundException;
import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.repository.LoyaltyAccountRepository;
import com.hoamai.loyalty_crm.pos.dto.MockPosCheckoutRequest;
import com.hoamai.loyalty_crm.pos.dto.MockPosCheckoutResponse;
import com.hoamai.loyalty_crm.store.entity.Store;
import com.hoamai.loyalty_crm.store.repository.StoreRepository;
import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionRequest;
import com.hoamai.loyalty_crm.transaction.dto.TransactionResponse;
import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import com.hoamai.loyalty_crm.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
public class MockPosController {

        private final StoreRepository storeRepository;
        private final CustomerRepository customerRepository;
        private final TransactionService transactionService;
        private final LoyaltyAccountRepository loyaltyAccountRepository;

        /**
         * Mock POS Integration API - Simulates external POS devices submitting sales
         * data to the Central Platform.
         * Guarantees BR-03 (Points only earned on SUCCESS) and BR-05 (Idempotency
         * duplicate prevention).
         */
        @PostMapping("/mock-checkout")
        public ResponseEntity<ApiResponse<MockPosCheckoutResponse>> mockPosCheckout(
                        @Valid @RequestBody MockPosCheckoutRequest request) {

                Store store = storeRepository.findByStoreCode(request.getStoreCode())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Store not found with code: " + request.getStoreCode()));

                Customer customer = null;
                if (request.getCustomerPhoneOrCode() != null && !request.getCustomerPhoneOrCode().isBlank()) {
                        customer = customerRepository.findByPhoneOrCustomerCode(request.getCustomerPhoneOrCode().trim())
                                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone/code: "
                                                        + request.getCustomerPhoneOrCode()));
                }

                CreateTransactionRequest createTxRequest = CreateTransactionRequest.builder()
                                .transactionCode(request.getPosTransactionId())
                                .customerId(customer != null ? customer.getId() : null)
                                .storeId(store.getId())
                                .pointsToRedeem(request.getPointsToRedeem())
                                .transactionDate(LocalDateTime.now())
                                .status(TransactionStatus.SUCCESS)
                                .items(request.getItems())
                                .build();

                TransactionResponse txResponse = transactionService.createTransaction(createTxRequest);

                Long currentBalance = 0L;
                if (customer != null) {
                        currentBalance = loyaltyAccountRepository.findByCustomerId(customer.getId())
                                        .map(acc -> acc.getPointBalance())
                                        .orElse(0L);
                }

                MockPosCheckoutResponse response = MockPosCheckoutResponse.builder()
                                .posDeviceId(request.getPosDeviceId())
                                .storeCode(store.getStoreCode())
                                .transactionCode(txResponse.getTransactionCode())
                                .customerCode(customer != null ? customer.getCustomerCode() : null)
                                .customerName(customer != null ? customer.getFullName() : "Khách lẻ")
                                .subtotalAmount(txResponse.getSubtotalAmount())
                                .discountAmount(txResponse.getDiscountAmount())
                                .pointsRedeemed(txResponse.getPointsRedeemed())
                                .totalAmount(txResponse.getTotalAmount())
                                .pointsEarned(txResponse.getPointsEarned() != null ? txResponse.getPointsEarned() : 0L)
                                .newPointBalance(currentBalance)
                                .isDuplicatePosTransaction(txResponse.getIsDuplicateRequest())
                                .timestamp(LocalDateTime.now())
                                .items(txResponse.getItems())
                                .build();

                HttpStatus status = Boolean.TRUE.equals(txResponse.getIsDuplicateRequest()) ? HttpStatus.OK
                                : HttpStatus.CREATED;
                String message = Boolean.TRUE.equals(txResponse.getIsDuplicateRequest())
                                ? "POS Transaction already processed (BR-05 Idempotent)."
                                : "POS Checkout processed successfully and loyalty points calculated.";

                return ResponseEntity.status(status).body(ApiResponse.success(message, response));
        }
}
