package lt.viko.eif.subtrack.dto;

import lt.viko.eif.subtrack.entity.BillingCycle;
import lt.viko.eif.subtrack.mapper.SpendingMapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response payload representing the authenticated user's subscription
 * spending summary, broken down by category.
 *
 * <p>All monetary amounts are expressed in the original currencies of each
 * subscription. Monthly costs are normalised using
 * {@link BillingCycle#getMonths()}; yearly costs are
 * derived by multiplying the monthly cost by twelve. Price calculations are
 * performed in the service layer; this record is assembled by
 * {@link SpendingMapper}.</p>
 *
 * @param categories per-category spending breakdowns
 */
public record SpendingSummaryResponse(List<CategorySummary> categories) {

    /**
     * Spending breakdown for a single category.
     *
     * @param categoryName  the name of the category
     * @param subscriptions the subscription entries within this category
     */
    public record CategorySummary(
            String categoryName,
            List<SubscriptionSummary> subscriptions
    ) {}

    /**
     * Spending summary for a single active subscription.
     *
     * <p>The {@code monthlyPrice} and {@code yearlyPrice} fields are
     * pre-calculated by the service layer and passed into the mapper; they
     * are not derived from the entity directly.</p>
     *
     * @param name          display name of the subscription
     * @param originalPrice original price per billing cycle as stored
     * @param currency      ISO 4217 currency code of the original price
     * @param monthlyPrice  price normalised to a monthly equivalent
     * @param yearlyPrice   yearly cost derived as monthly × 12
     */
    public record SubscriptionSummary(
            String name,
            BigDecimal originalPrice,
            String currency,
            BigDecimal monthlyPrice,
            BigDecimal yearlyPrice
    ) {}
}
