package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.SpendingSummaryResponse;

/**
 * Service for computing subscription spending summaries for the current user.
 */
public interface SpendingService {

    /**
     * Returns a spending summary for the currently authenticated user.
     *
     * <p>Only active subscriptions are included. Prices are normalised to
     * a monthly equivalent using each subscription's billing cycle, and a
     * yearly figure is derived by multiplying the monthly cost by twelve.
     * Results are grouped by category.</p>
     *
     * @return the spending summary
     */
    SpendingSummaryResponse getSummaryForCurrentUser();
}