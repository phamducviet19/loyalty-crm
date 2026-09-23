package com.hoamai.loyalty_crm.loyalty.dto;

import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyAccountDetailResponse {

    private UUID id;
    private UUID customerId;
    private String customerCode;
    private String customerName;
    private Long pointBalance;

    private LoyaltyTier tier;
    private String tierName;
    private LoyaltyTier nextTier;
    private Long pointsNeededForNextTier;
    private Double pointMultiplier;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
