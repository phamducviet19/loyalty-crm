package com.hoamai.loyalty_crm.transaction.dto;

import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private String transactionCode;

    private UUID customerId;
    private String customerCode;
    private String customerName;

    private UUID storeId;
    private String storeCode;
    private String storeName;

    private LocalDateTime transactionDate;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private Long pointsRedeemed;
    private BigDecimal totalAmount;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    private List<TransactionItemResponse> items;
    private Long pointsEarned;
    private Boolean isDuplicateRequest;
}
