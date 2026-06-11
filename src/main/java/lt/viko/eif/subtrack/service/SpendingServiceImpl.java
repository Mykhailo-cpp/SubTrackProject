package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.SpendingSummaryResponse;
import lt.viko.eif.subtrack.dto.SpendingSummaryResponse.CategorySummary;
import lt.viko.eif.subtrack.dto.SpendingSummaryResponse.SubscriptionSummary;
import lt.viko.eif.subtrack.entity.BillingCycle;
import lt.viko.eif.subtrack.entity.Subscription;
import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.mapper.SpendingMapper;
import lt.viko.eif.subtrack.repository.SubscriptionRepository;
import lt.viko.eif.subtrack.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default {@link SpendingService} implementation.
 *
 * <p>Active subscriptions for the current user are fetched, normalised to a
 * monthly cost using {@link BillingCycle#getMonths()}, then grouped by
 * category name. Weekly subscriptions (months == 0) are approximated as
 * {@value #WEEKS_PER_MONTH} weeks per month. All price calculations happen
 * here; {@link SpendingMapper} is responsible only for assembling the
 * resulting record DTOs.</p>
 */
@Service
@Transactional(readOnly = true)
public class SpendingServiceImpl implements SpendingService {

    /** Weeks approximated per month for weekly billing cycles. */
    private static final BigDecimal WEEKS_PER_MONTH = new BigDecimal("4.33");

    /** Multiplier used to derive yearly cost from monthly cost. */
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    /** Monetary scale applied to all calculated amounts. */
    private static final int MONETARY_SCALE = 2;

    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SpendingMapper spendingMapper;

    /**
     * Creates the service.
     *
     * @param subscriptionRepository the subscription repository
     * @param currentUserProvider    the current-user provider
     * @param spendingMapper         the spending mapper
     */
    public SpendingServiceImpl(SubscriptionRepository subscriptionRepository,
                               CurrentUserProvider currentUserProvider,
                               SpendingMapper spendingMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserProvider = currentUserProvider;
        this.spendingMapper = spendingMapper;
    }

    /** {@inheritDoc} */
    @Override
    public SpendingSummaryResponse getSummaryForCurrentUser() {
        User currentUser = currentUserProvider.getCurrentUser();

        List<Subscription> active =
                subscriptionRepository.findByUserIdAndActiveTrue(currentUser.getId());

        Map<String, List<Subscription>> byCategory = active.stream()
                .collect(Collectors.groupingBy(s -> s.getCategory().getName()));

        List<CategorySummary> categories = byCategory.entrySet().stream()
                .map(entry -> {
                    List<SubscriptionSummary> summaries = entry.getValue().stream()
                            .map(this::toSubscriptionSummary)
                            .collect(Collectors.toList());
                    return spendingMapper.toCategorySummary(entry.getKey(), summaries);
                })
                .collect(Collectors.toList());

        return new SpendingSummaryResponse(categories);
    }

    /**
     * Calculates normalised costs for a subscription and delegates assembly
     * to {@link SpendingMapper}.
     *
     * @param subscription the subscription entity
     * @return the assembled subscription summary record
     */
    private SubscriptionSummary toSubscriptionSummary(Subscription subscription) {
        BigDecimal monthly = normaliseToMonthly(
                subscription.getPrice(), subscription.getBillingCycle());
        BigDecimal yearly = monthly.multiply(MONTHS_PER_YEAR)
                .setScale(MONETARY_SCALE, RoundingMode.HALF_UP);

        return spendingMapper.toSubscriptionSummary(subscription, monthly, yearly);
    }

    /**
     * Normalises a price to its monthly equivalent.
     *
     * <p>For monthly cycles the price is returned unchanged. For multi-month
     * cycles (e.g. ANNUAL) the price is divided by the number of months.
     * For weekly cycles the price is multiplied by {@value #WEEKS_PER_MONTH}.</p>
     *
     * @param price        the original price per billing cycle
     * @param billingCycle the billing frequency
     * @return the normalised monthly price
     */
    private BigDecimal normaliseToMonthly(BigDecimal price, BillingCycle billingCycle) {
        if (billingCycle == BillingCycle.WEEKLY) {
            return price.multiply(WEEKS_PER_MONTH)
                    .setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        }
        int months = billingCycle.getMonths();
        if (months <= 1) {
            return price.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        }
        return price.divide(BigDecimal.valueOf(months), MONETARY_SCALE, RoundingMode.HALF_UP);
    }
}
