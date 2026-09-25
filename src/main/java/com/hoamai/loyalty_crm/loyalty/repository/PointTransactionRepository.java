package com.hoamai.loyalty_crm.loyalty.repository;

import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID>, JpaSpecificationExecutor<PointTransaction> {

    List<PointTransaction> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}
