package com.hoamai.loyalty_crm.customer.dto;

import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSegmentResponse {

    private UUID id;
    private String customerCode;
    private String fullName;
    private String phone;
    private String email;
    private LoyaltyTier tier;
    private String tierName;
    private Long pointBalance;
    private BigDecimal totalSpend;
    private long totalTransactions;
    private LocalDateTime createdAt;
}
