package com.hoamai.loyalty_crm.customer.repository;

import com.hoamai.loyalty_crm.customer.entity.Customer;
import com.hoamai.loyalty_crm.loyalty.entity.LoyaltyTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByCustomerCode(String customerCode);

    @Query("SELECT c FROM Customer c WHERE c.phone = :identifier OR c.customerCode = :identifier")
    Optional<Customer> findByPhoneOrCustomerCode(@Param("identifier") String identifier);

    @Query("SELECT DISTINCT c FROM Customer c " +
           "LEFT JOIN LoyaltyAccount la ON la.customer.id = c.id " +
           "WHERE (:minPoints IS NULL OR la.pointBalance >= :minPoints) " +
           "AND (:maxPoints IS NULL OR la.pointBalance <= :maxPoints) " +
           "ORDER BY c.createdAt DESC")
    Page<Customer> findCustomersByPointsRange(
            @Param("minPoints") Long minPoints,
            @Param("maxPoints") Long maxPoints,
            Pageable pageable);
}
