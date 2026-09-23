package com.hoamai.loyalty_crm.loyalty.entity;

import lombok.Getter;

@Getter
public enum LoyaltyTier {
    BRONZE("Bronze", 0L, 999L, 1.0),
    SILVER("Silver", 1000L, 4999L, 1.1),
    GOLD("Gold", 5000L, 19999L, 1.25),
    PLATINUM("Platinum", 20000L, Long.MAX_VALUE, 1.5);

    private final String displayName;
    private final long minPoints;
    private final long maxPoints;
    private final double pointMultiplier;

    LoyaltyTier(String displayName, long minPoints, long maxPoints, double pointMultiplier) {
        this.displayName = displayName;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.pointMultiplier = pointMultiplier;
    }

    public static LoyaltyTier fromPoints(long points) {
        if (points >= PLATINUM.minPoints) return PLATINUM;
        if (points >= GOLD.minPoints) return GOLD;
        if (points >= SILVER.minPoints) return SILVER;
        return BRONZE;
    }

    public Long getPointsNeededForNextTier(long currentPoints) {
        if (this == PLATINUM) {
            return 0L;
        }
        if (this == BRONZE) {
            return SILVER.minPoints - currentPoints;
        }
        if (this == SILVER) {
            return GOLD.minPoints - currentPoints;
        }
        if (this == GOLD) {
            return PLATINUM.minPoints - currentPoints;
        }
        return 0L;
    }

    public LoyaltyTier getNextTier() {
        if (this == BRONZE) return SILVER;
        if (this == SILVER) return GOLD;
        if (this == GOLD) return PLATINUM;
        return PLATINUM;
    }
}
