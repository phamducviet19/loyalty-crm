package com.hoamai.loyalty_crm.customer.service;

import com.hoamai.loyalty_crm.common.exception.DuplicateResourceException;
import com.hoamai.loyalty_crm.common.exception.ResourceNotFoundException;
import com.hoamai.loyalty_crm.customer.dto.*;
import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.customer.repository.CustomerRepository;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyAccount;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import com.hoamai.loyalty_crm.loyalty.repository.LoyaltyAccountRepository;
import com.hoamai.loyalty_crm.loyalty.repository.PointTransactionRepository;
import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import com.hoamai.loyalty_crm.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public CustomerResponse registerCustomer(RegisterCustomerRequest request) {
        if (customerRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone number already registered: " + request.getPhone());
        }

        String email = normalizeEmail(request.getEmail());
        if (email != null && customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        String customerCode = request.getCustomerCode();
        if (customerCode != null && !customerCode.isBlank()) {
            customerCode = customerCode.trim();
            if (customerRepository.existsByCustomerCode(customerCode)) {
                throw new DuplicateResourceException("Customer code already exists: " + customerCode);
            }
        } else {
            customerCode = generateUniqueCustomerCode();
        }

        Customer customer = Customer.builder()
                .customerCode(customerCode)
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .email(email)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender() != null && !request.getGender().isBlank() ? request.getGender().trim() : null)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        // Auto-create loyalty account for new customer
        LoyaltyAccount loyaltyAccount = LoyaltyAccount.builder()
                .customer(savedCustomer)
                .pointBalance(0L)
                .build();
        loyaltyAccountRepository.save(loyaltyAccount);

        return mapToCustomerResponse(savedCustomer);
    }

    @Override
    public CustomerIdentifyResponse identifyCustomer(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        Customer customer = findCustomerByIdentifier(query.trim());

        Long pointBalance = loyaltyAccountRepository.findByCustomerId(customer.getId())
                .map(LoyaltyAccount::getPointBalance)
                .orElse(0L);

        return CustomerIdentifyResponse.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .pointBalance(pointBalance)
                .createdAt(customer.getCreatedAt())
                .build();
    }

    @Override
    public CustomerResponse getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToCustomerResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().trim().equals(customer.getPhone())) {
            String newPhone = request.getPhone().trim();
            if (customerRepository.existsByPhone(newPhone)) {
                throw new DuplicateResourceException("Phone number already registered: " + newPhone);
            }
            customer.setPhone(newPhone);
        }

        if (request.getEmail() != null) {
            String sanitizedEmail = normalizeEmail(request.getEmail());
            if (sanitizedEmail != null && !sanitizedEmail.equalsIgnoreCase(customer.getEmail())) {
                if (customerRepository.existsByEmail(sanitizedEmail)) {
                    throw new DuplicateResourceException("Email already registered: " + sanitizedEmail);
                }
            }
            customer.setEmail(sanitizedEmail);
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            customer.setFullName(request.getFullName().trim());
        }

        if (request.getDateOfBirth() != null) {
            customer.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            customer.setGender(request.getGender().isBlank() ? null : request.getGender().trim());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToCustomerResponse(updatedCustomer);
    }

    @Override
    public Customer360Response getCustomer360(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        // Loyalty Account
        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByCustomerId(id)
                .orElse(null);
        Customer360Response.LoyaltyAccountSummary loyaltySummary = null;
        if (loyaltyAccount != null) {
            loyaltySummary = Customer360Response.LoyaltyAccountSummary.builder()
                    .id(loyaltyAccount.getId())
                    .pointBalance(loyaltyAccount.getPointBalance())
                    .createdAt(loyaltyAccount.getCreatedAt())
                    .updatedAt(loyaltyAccount.getUpdatedAt())
                    .build();
        }

        // Metrics
        BigDecimal totalSpend = transactionRepository.sumTotalAmountByCustomerId(id);
        long totalCount = transactionRepository.countByCustomerId(id);
        Customer360Response.CustomerMetricsSummary metricsSummary = Customer360Response.CustomerMetricsSummary.builder()
                .totalSpendAmount(totalSpend != null ? totalSpend : BigDecimal.ZERO)
                .totalTransactionCount(totalCount)
                .build();

        // Recent Point Transactions (Top 10)
        List<PointTransaction> pointTxList = pointTransactionRepository
                .findByCustomerIdOrderByCreatedAtDesc(id, PageRequest.of(0, 10));
        List<Customer360Response.PointTransactionSummary> recentPointTxSummaries = pointTxList.stream()
                .map(pt -> Customer360Response.PointTransactionSummary.builder()
                        .id(pt.getId())
                        .type(pt.getType())
                        .points(pt.getPoints())
                        .description(pt.getDescription())
                        .transactionId(pt.getTransaction() != null ? pt.getTransaction().getId() : null)
                        .createdAt(pt.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        // Recent Transactions (Top 10)
        List<Transaction> txList = transactionRepository
                .findByCustomerIdOrderByTransactionDateDesc(id, PageRequest.of(0, 10));
        List<Customer360Response.TransactionSummary> recentTxSummaries = txList.stream()
                .map(t -> Customer360Response.TransactionSummary.builder()
                        .id(t.getId())
                        .transactionCode(t.getTransactionCode())
                        .storeCode(t.getStore() != null ? t.getStore().getStoreCode() : null)
                        .storeName(t.getStore() != null ? t.getStore().getStoreName() : null)
                        .transactionDate(t.getTransactionDate())
                        .totalAmount(t.getTotalAmount())
                        .status(t.getStatus())
                        .itemCount(t.getItems() != null ? t.getItems().size() : 0)
                        .build())
                .collect(Collectors.toList());

        return Customer360Response.builder()
                .profile(mapToCustomerResponse(customer))
                .loyaltyAccount(loyaltySummary)
                .metrics(metricsSummary)
                .recentPointTransactions(recentPointTxSummaries)
                .recentTransactions(recentTxSummaries)
                .build();
    }

    @Override
    public Page<CustomerSegmentResponse> getSegmentedCustomers(
            LoyaltyTier tier,
            BigDecimal minSpend,
            BigDecimal maxSpend,
            Pageable pageable) {

        Long minPoints = tier != null ? tier.getMinPoints() : null;
        Long maxPoints = tier != null ? tier.getMaxPoints() : null;

        Page<Customer> customerPage = customerRepository.findCustomersByPointsRange(minPoints, maxPoints, pageable);

        List<CustomerSegmentResponse> content = customerPage.getContent().stream()
                .map(customer -> {
                    LoyaltyAccount account = loyaltyAccountRepository.findByCustomerId(customer.getId()).orElse(null);
                    long pointBalance = account != null ? account.getPointBalance() : 0L;
                    LoyaltyTier currentTier = LoyaltyTier.fromPoints(pointBalance);

                    BigDecimal totalSpend = transactionRepository.sumTotalAmountByCustomerId(customer.getId());
                    if (totalSpend == null) totalSpend = BigDecimal.ZERO;

                    long totalTransactions = transactionRepository.countByCustomerId(customer.getId());

                    return CustomerSegmentResponse.builder()
                            .id(customer.getId())
                            .customerCode(customer.getCustomerCode())
                            .fullName(customer.getFullName())
                            .phone(customer.getPhone())
                            .email(customer.getEmail())
                            .tier(currentTier)
                            .tierName(currentTier.getDisplayName())
                            .pointBalance(pointBalance)
                            .totalSpend(totalSpend)
                            .totalTransactions(totalTransactions)
                            .createdAt(customer.getCreatedAt())
                            .build();
                })
                .filter(resp -> (minSpend == null || resp.getTotalSpend().compareTo(minSpend) >= 0)
                        && (maxSpend == null || resp.getTotalSpend().compareTo(maxSpend) <= 0))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, customerPage.getTotalElements());
    }

    private Customer findCustomerByIdentifier(String query) {
        try {
            UUID uuid = UUID.fromString(query);
            Optional<Customer> customerById = customerRepository.findById(uuid);
            if (customerById.isPresent()) {
                return customerById.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, proceed with phone or customer code search
        }

        return customerRepository.findByPhoneOrCustomerCode(query)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with phone/code/id: " + query));
    }

    private String generateUniqueCustomerCode() {
        String code;
        do {
            code = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (customerRepository.existsByCustomerCode(code));
        return code;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
