package com.hoamai.loyalty_crm.pos.dto;

import com.hoamai.loyalty_crm.transaction.dto.TransactionItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockPosCheckoutResponse {

    private String posDeviceId;
    private String storeCode;
    private String transactionCode;
    private String customerCode;
    private String customerName;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private Long pointsRedeemed;
    private BigDecimal totalAmount;
    private Long pointsEarned;
    private Long newPointBalance;
    private Boolean isDuplicatePosTransaction;
    private LocalDateTime timestamp;
    private List<TransactionItemResponse> items;
}
