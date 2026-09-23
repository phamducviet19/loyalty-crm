package com.hoamai.loyalty_crm.transaction.dto;

import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    @NotBlank(message = "Transaction code (Idempotency Key) is required")
    @Size(max = 100, message = "Transaction code must not exceed 100 characters")
    private String transactionCode;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Store ID is required")
    private UUID storeId;

    private LocalDateTime transactionDate;

    private TransactionStatus status;

    @NotEmpty(message = "Transaction items must not be empty")
    @Valid
    private List<CreateTransactionItemRequest> items;
}
