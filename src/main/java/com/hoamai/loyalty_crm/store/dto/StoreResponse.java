package com.hoamai.loyalty_crm.store.dto;

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
public class StoreResponse {

    private UUID id;
    private String storeCode;
    private String storeName;
    private String address;
    private LocalDateTime createdAt;
}
