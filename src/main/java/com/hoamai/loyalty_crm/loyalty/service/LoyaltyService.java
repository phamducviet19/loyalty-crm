package com.hoamai.loyalty_crm.loyalty.service;

import com.hoamai.loyalty_crm.loyalty.dto.AdjustPointRequest;
import com.hoamai.loyalty_crm.loyalty.dto.LoyaltyAccountDetailResponse;
import com.hoamai.loyalty_crm.loyalty.dto.PointTransactionResponse;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransaction;
import com.hoamai.loyalty_crm.loyalty.entity.PointTransactionType;
import com.hoamai.loyalty_crm.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LoyaltyService {

    PointTransaction earnPointsForTransaction(Transaction transaction);

    PointTransactionResponse adjustPoints(AdjustPointRequest request);

    LoyaltyAccountDetailResponse getLoyaltyAccountDetail(UUID customerId);

    Page<PointTransactionResponse> getPointHistory(UUID customerId, PointTransactionType type, Pageable pageable);
}
