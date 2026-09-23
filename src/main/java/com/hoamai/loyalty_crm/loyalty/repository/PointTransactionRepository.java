package com.hoamai.loyalty_crm.loyalty.repository;

import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    List<PointTransaction> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query("SELECT pt FROM PointTransaction pt WHERE " +
           "(:customerId IS NULL OR pt.customer.id = :customerId) AND " +
           "(:type IS NULL OR pt.type = :type) " +
           "ORDER BY pt.createdAt DESC")
    Page<PointTransaction> findPointTransactions(
            @Param("customerId") UUID customerId,
            @Param("type") PointTransactionType type,
            Pageable pageable);
}
