package com.hoamai.loyalty_crm.transaction.repository;

import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionCode(String transactionCode);

    boolean existsByTransactionCode(String transactionCode);

    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(UUID customerId, Pageable pageable);

    long countByCustomerId(UUID customerId);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.customer.id = :customerId AND t.status = 'SUCCESS'")
    BigDecimal sumTotalAmountByCustomerId(@Param("customerId") UUID customerId);
}
