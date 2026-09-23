package com.hoamai.loyalty_crm.loyalty.controller;

import com.hoamai.loyalty_crm.common.dto.ApiResponse;
import com.hoamai.loyalty_crm.loyalty.dto.AdjustPointRequest;
import com.hoamai.loyalty_crm.loyalty.dto.LoyaltyAccountDetailResponse;
import com.hoamai.loyalty_crm.loyalty.dto.PointTransactionResponse;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
import com.hoamai.loyalty_crm.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    /**
     * Loyalty Balance & Tier Details API.
     */
    @GetMapping("/accounts/{customerId}")
    public ResponseEntity<ApiResponse<LoyaltyAccountDetailResponse>> getLoyaltyAccountDetail(
            @PathVariable("customerId") UUID customerId) {
        LoyaltyAccountDetailResponse response = loyaltyService.getLoyaltyAccountDetail(customerId);
        return ResponseEntity.ok(ApiResponse.success("Loyalty account details fetched successfully", response));
    }

    /**
     * Point History API - Filtered and paginated point log.
     */
    @GetMapping("/point-transactions")
    public ResponseEntity<ApiResponse<Page<PointTransactionResponse>>> getPointHistory(
            @RequestParam(name = "customerId", required = false) UUID customerId,
            @RequestParam(name = "type", required = false) PointTransactionType type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<PointTransactionResponse> response = loyaltyService.getPointHistory(customerId, type, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Manual Point Adjustment API - Accessible by Store Managers / Admins.
     */
    @PostMapping("/adjust-points")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'CRM_ADMIN') or permitAll()")
    public ResponseEntity<ApiResponse<PointTransactionResponse>> adjustPoints(
            @Valid @RequestBody AdjustPointRequest request) {
        PointTransactionResponse response = loyaltyService.adjustPoints(request);
        return ResponseEntity.ok(ApiResponse.success("Point balance adjusted successfully", response));
    }
}
