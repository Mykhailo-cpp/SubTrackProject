package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.SpendingSummaryResponse;
import lt.viko.eif.subtrack.dto.SpendingSummaryResponse.CategorySummary;
import lt.viko.eif.subtrack.dto.SpendingSummaryResponse.SubscriptionSummary;
import lt.viko.eif.subtrack.entity.BillingCycle;
import lt.viko.eif.subtrack.entity.Category;
import lt.viko.eif.subtrack.entity.Subscription;
import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.mapper.SpendingMapper;
import lt.viko.eif.subtrack.repository.SubscriptionRepository;
import lt.viko.eif.subtrack.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SpendingServiceImpl}. All collaborators
 * ({@link SubscriptionRepository}, {@link CurrentUserProvider},
 * {@link SpendingMapper}) are mocked with Mockito; no Spring context or
 * database is involved. The focus is the service's own logic: monthly/yearly
 * normalisation per billing cycle and grouping by category.
 */
@ExtendWith(MockitoExtension.class)
class SpendingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private SpendingMapper spendingMapper;

    @InjectMocks
    private SpendingServiceImpl spendingService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("alice", "alice@example.com", "hashed");
        currentUser.setId(1L);
    }

    /** Builds an active subscription in the given category. */
    private Subscription subscription(String name, BigDecimal price,
                                      BillingCycle cycle, String categoryName) {
        Category category = new Category(categoryName, categoryName + " services");
        Subscription s = new Subscription(
                name, "desc", price, "EUR", cycle, LocalDate.of(2026, 7, 1), true);
        s.setUser(currentUser);
        s.setCategory(category);
        return s;
    }

    @Test
    void getSummary_NoActiveSubscriptions_ReturnsEmptyCategories() {
        // Arrange
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(subscriptionRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of());

        // Act
        SpendingSummaryResponse result = spendingService.getSummaryForCurrentUser();

        // Assert
        assertTrue(result.categories().isEmpty());
        verify(spendingMapper, times(0)).toCategorySummary(anyString(), anyList());
    }

    @Nested
    class Normalisation {

        @Test
        void monthlyCycle_PriceUnchanged_YearlyIsTwelveTimes() {
            // Arrange: 10.00 / month -> monthly 10.00, yearly 120.00
            Subscription sub = subscription("Netflix", new BigDecimal("10.00"),
                    BillingCycle.MONTHLY, "Streaming");
            stubSingle(sub);

            // Act
            spendingService.getSummaryForCurrentUser();

            // Assert
            assertNormalised(sub, "10.00", "120.00");
        }

        @Test
        void annualCycle_DividedAcrossTwelveMonths() {
            // Arrange: 120.00 / year -> monthly 10.00, yearly 120.00
            Subscription sub = subscription("Prime", new BigDecimal("120.00"),
                    BillingCycle.ANNUAL, "Shopping");
            stubSingle(sub);

            // Act
            spendingService.getSummaryForCurrentUser();

            // Assert
            assertNormalised(sub, "10.00", "120.00");
        }

        @Test
        void weeklyCycle_MultipliedByWeeksPerMonth() {
            // Arrange: 2.00 / week -> monthly 2.00 * 4.33 = 8.66, yearly 103.92
            Subscription sub = subscription("Paper", new BigDecimal("2.00"),
                    BillingCycle.WEEKLY, "News");
            stubSingle(sub);

            // Act
            spendingService.getSummaryForCurrentUser();

            // Assert
            assertNormalised(sub, "8.66", "103.92");
        }

        @Test
        void quarterlyCycle_DividedAcrossThreeMonths() {
            // Arrange: 30.00 / quarter -> monthly 10.00, yearly 120.00
            Subscription sub = subscription("Gym", new BigDecimal("30.00"),
                    BillingCycle.QUARTERLY, "Health");
            stubSingle(sub);

            // Act
            spendingService.getSummaryForCurrentUser();

            // Assert
            assertNormalised(sub, "10.00", "120.00");
        }
    }

    @Nested
    class Grouping {

        @Test
        void subscriptionsInDifferentCategories_ProduceOneSummaryEach() {
            // Arrange: two subscriptions in two distinct categories
            Subscription netflix = subscription("Netflix", new BigDecimal("10.00"),
                    BillingCycle.MONTHLY, "Streaming");
            Subscription gym = subscription("Gym", new BigDecimal("30.00"),
                    BillingCycle.QUARTERLY, "Health");

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findByUserIdAndActiveTrue(1L))
                    .thenReturn(List.of(netflix, gym));
            when(spendingMapper.toSubscriptionSummary(any(Subscription.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new SubscriptionSummary("x", BigDecimal.ONE, "EUR",
                            BigDecimal.ONE, BigDecimal.ONE));
            when(spendingMapper.toCategorySummary(anyString(), anyList()))
                    .thenReturn(new CategorySummary("c", List.of()));

            // Act
            spendingService.getSummaryForCurrentUser();

            // Assert: one category summary per distinct category name
            verify(spendingMapper).toCategorySummary(eq("Streaming"), anyList());
            verify(spendingMapper).toCategorySummary(eq("Health"), anyList());
            verify(spendingMapper, times(2)).toCategorySummary(anyString(), anyList());
        }

        @Test
        void subscriptionsInSameCategory_AreGroupedTogether() {
            // Arrange: two subscriptions sharing one category
            Subscription netflix = subscription("Netflix", new BigDecimal("10.00"),
                    BillingCycle.MONTHLY, "Streaming");
            Subscription spotify = subscription("Spotify", new BigDecimal("9.99"),
                    BillingCycle.MONTHLY, "Streaming");

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findByUserIdAndActiveTrue(1L))
                    .thenReturn(List.of(netflix, spotify));
            when(spendingMapper.toSubscriptionSummary(any(Subscription.class),
                    any(BigDecimal.class), any(BigDecimal.class)))
                    .thenReturn(new SubscriptionSummary("x", BigDecimal.ONE, "EUR",
                            BigDecimal.ONE, BigDecimal.ONE));
            when(spendingMapper.toCategorySummary(anyString(), anyList()))
                    .thenReturn(new CategorySummary("c", List.of()));

            // Act
            spendingService.getSummaryForCurrentUser();

            // Assert: a single category summary built from a list of two entries
            ArgumentCaptor<List<SubscriptionSummary>> listCaptor = listCaptor();
            verify(spendingMapper, times(1)).toCategorySummary(eq("Streaming"), listCaptor.capture());
            assertEquals(2, listCaptor.getValue().size());
            verify(spendingMapper, times(2)).toSubscriptionSummary(
                    any(Subscription.class), any(BigDecimal.class), any(BigDecimal.class));
        }
    }

    // --- helpers ---

    /** Stubs the common path for a single-subscription scenario. */
    private void stubSingle(Subscription sub) {
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(subscriptionRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(sub));
        when(spendingMapper.toSubscriptionSummary(any(Subscription.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new SubscriptionSummary("x", BigDecimal.ONE, "EUR",
                        BigDecimal.ONE, BigDecimal.ONE));
        when(spendingMapper.toCategorySummary(anyString(), anyList()))
                .thenReturn(new CategorySummary("c", List.of()));
    }

    /** Verifies the monthly/yearly amounts passed to the mapper for one subscription. */
    private void assertNormalised(Subscription sub, String expectedMonthly, String expectedYearly) {
        ArgumentCaptor<BigDecimal> monthlyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> yearlyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(spendingMapper).toSubscriptionSummary(
                eq(sub), monthlyCaptor.capture(), yearlyCaptor.capture());
        assertEquals(0, new BigDecimal(expectedMonthly).compareTo(monthlyCaptor.getValue()),
                "monthly mismatch");
        assertEquals(0, new BigDecimal(expectedYearly).compareTo(yearlyCaptor.getValue()),
                "yearly mismatch");
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<SubscriptionSummary>> listCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}