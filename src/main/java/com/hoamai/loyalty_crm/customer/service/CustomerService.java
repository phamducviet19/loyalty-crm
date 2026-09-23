package com.hoamai.loyalty_crm.customer.service;

import com.hoamai.loyalty_crm.customer.dto.*;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface CustomerService {

    CustomerResponse registerCustomer(RegisterCustomerRequest request);

    CustomerIdentifyResponse identifyCustomer(String query);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request);

    Customer360Response getCustomer360(UUID id);

    Page<CustomerSegmentResponse> getSegmentedCustomers(
            LoyaltyTier tier,
            BigDecimal minSpend,
            BigDecimal maxSpend,
            Pageable pageable);
}
