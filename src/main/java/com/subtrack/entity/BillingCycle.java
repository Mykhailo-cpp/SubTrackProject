package com.subtrack.entity;

/**
 * Enumerates the supported billing frequencies for a {@link Subscription}.
 *
 * <p>The {@code months} value attached to each constant expresses how many
 * calendar months the cycle spans, which is convenient when normalising
 * recurring costs to a common period (for example, computing a monthly
 * spending summary).</p>
 */
public enum BillingCycle {

    /** Billed once per week (treated as roughly a quarter of a month for cost normalisation). */
    WEEKLY(0),

    /** Billed once per calendar month. */
    MONTHLY(1),

    /** Billed once every three months. */
    QUARTERLY(3),

    /** Billed once every six months. */
    SEMI_ANNUAL(6),

    /** Billed once per year. */
    ANNUAL(12);

    /** Number of months the billing cycle spans. */
    private final int months;

    /**
     * Creates a billing cycle constant.
     *
     * @param months the number of calendar months the cycle spans
     */
    BillingCycle(int months) {
        this.months = months;
    }

    /**
     * Returns the number of calendar months this cycle spans.
     *
     * @return the span of the cycle in months
     */
    public int getMonths() {
        return months;
    }
}