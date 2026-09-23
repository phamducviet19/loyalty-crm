package com.hoamai.loyalty_crm.loyalty.dto;

import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
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
public class PointTransactionResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private UUID transactionId;
    private PointTransactionType type;
    private Long points;
    private String description;
    private LocalDateTime createdAt;
}
