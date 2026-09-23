package com.hoamai.loyalty_crm.transaction.repository;

import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import com.hoamai.loyalty_crm.transaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(UUID customerId, Pageable pageable);

    long countByCustomerId(UUID customerId);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.customer.id = :customerId AND t.status = 'SUCCESS'")
    BigDecimal sumTotalAmountByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:customerId IS NULL OR t.customer.id = :customerId) AND " +
           "(:storeId IS NULL OR t.store.id = :storeId) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:fromDate IS NULL OR t.transactionDate >= :fromDate) AND " +
           "(:toDate IS NULL OR t.transactionDate <= :toDate) " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findTransactions(
            @Param("customerId") UUID customerId,
            @Param("storeId") UUID storeId,
            @Param("status") TransactionStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
