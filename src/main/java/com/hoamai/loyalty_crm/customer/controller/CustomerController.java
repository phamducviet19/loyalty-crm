package com.hoamai.loyalty_crm.customer.controller;

import com.hoamai.loyalty_crm.common.dto.ApiResponse;
import com.hoamai.loyalty_crm.customer.dto.*;
import com.hoamai.loyalty_crm.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Customer Registration API - Allows Store Staff to register a new Customer.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {
        CustomerResponse response = customerService.registerCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Customer registered successfully", response));
    }

    /**
     * Customer Identification API - Identify Customer by Phone Number, Customer Code, or ID.
     */
    @GetMapping("/identify")
    public ResponseEntity<ApiResponse<CustomerIdentifyResponse>> identifyCustomer(
            @RequestParam("query") String query) {
        CustomerIdentifyResponse response = customerService.identifyCustomer(query);
        return ResponseEntity.ok(ApiResponse.success("Customer identified successfully", response));
    }

    /**
     * View Customer Profile API.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(
            @PathVariable("id") UUID id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update Customer Profile API.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    /**
     * Customer 360 API - Aggregated view displaying Profile, Transaction, and Loyalty information.
     */
    @GetMapping("/{id}/360")
    public ResponseEntity<ApiResponse<Customer360Response>> getCustomer360(
            @PathVariable("id") UUID id) {
        Customer360Response response = customerService.getCustomer360(id);
        return ResponseEntity.ok(ApiResponse.success("Customer 360 data fetched successfully", response));
    }
}
