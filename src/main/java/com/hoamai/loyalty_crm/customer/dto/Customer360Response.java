package com.hoamai.loyalty_crm.customer.dto;

import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
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
public class Customer360Response {

    private CustomerResponse profile;
    private LoyaltyAccountSummary loyaltyAccount;
    private CustomerMetricsSummary metrics;
    private List<PointTransactionSummary> recentPointTransactions;
    private List<TransactionSummary> recentTransactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoyaltyAccountSummary {
        private UUID id;
        private Long pointBalance;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerMetricsSummary {
        private BigDecimal totalSpendAmount;
        private long totalTransactionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PointTransactionSummary {
        private UUID id;
        private PointTransactionType type;
        private Long points;
        private String description;
        private UUID transactionId;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionSummary {
        private UUID id;
        private String transactionCode;
        private String storeCode;
        private String storeName;
        private LocalDateTime transactionDate;
        private BigDecimal totalAmount;
        private TransactionStatus status;
        private int itemCount;
    }
}
