package com.hoamai.loyalty_crm.loyalty.dto;

import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustPointRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Points value is required")
    private Long points;

    @NotNull(message = "Transaction type is required (EARN, REDEEM, ADJUSTMENT)")
    private PointTransactionType type;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
