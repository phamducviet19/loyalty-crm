package com.hoamai.loyalty_crm.customer.controller;

import com.hoamai.loyalty_crm.common.dto.ApiResponse;
import com.hoamai.loyalty_crm.customer.dto.CustomerSegmentResponse;
import com.hoamai.loyalty_crm.customer.service.CustomerService;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final CustomerService customerService;

    /**
     * Customer Segmentation API for Marketing Staff.
     * Allows filtering and segmenting customer base by Loyalty Tier, total spend range, and page size.
     */
    @GetMapping("/segmentation")
    @PreAuthorize("hasAnyRole('MARKETING_STAFF', 'SYSTEM_ADMINISTRATOR') or permitAll()")
    public ResponseEntity<ApiResponse<Page<CustomerSegmentResponse>>> getSegmentedCustomers(
            @RequestParam(name = "tier", required = false) LoyaltyTier tier,
            @RequestParam(name = "minSpend", required = false) BigDecimal minSpend,
            @RequestParam(name = "maxSpend", required = false) BigDecimal maxSpend,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Page<CustomerSegmentResponse> response = customerService.getSegmentedCustomers(
                tier, minSpend, maxSpend, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Customer segmentation data retrieved successfully", response));
    }
}
