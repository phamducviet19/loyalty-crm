package com.hoamai.loyalty_crm.store.repository;

import com.hoamai.loyalty_crm.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByStoreCode(String storeCode);

    boolean existsByStoreCode(String storeCode);
}
