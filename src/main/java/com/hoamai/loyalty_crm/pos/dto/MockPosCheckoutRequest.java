package com.hoamai.loyalty_crm.pos.dto;

import com.hoamai.loyalty_crm.transaction.dto.CreateTransactionItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockPosCheckoutRequest {

    @NotBlank(message = "POS device ID is required")
    private String posDeviceId;

    @NotBlank(message = "Store code is required")
    private String storeCode;

    private String customerPhoneOrCode;

    @NotBlank(message = "POS Transaction ID (Idempotency Key) is required")
    private String posTransactionId;

    @NotEmpty(message = "Checkout items must not be empty")
    @Valid
    private List<CreateTransactionItemRequest> items;
}
