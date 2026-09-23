package com.hoamai.loyalty_crm.customer.dto;

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
public class CustomerIdentifyResponse {

    private UUID id;
    private String customerCode;
    private String fullName;
    private String phone;
    private String email;
    private Long pointBalance;
    private LocalDateTime createdAt;
}
