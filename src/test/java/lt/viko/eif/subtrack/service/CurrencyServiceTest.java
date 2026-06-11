package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.CurrencyConversionResponse;
import lt.viko.eif.subtrack.entity.BillingCycle;
import lt.viko.eif.subtrack.entity.Category;
import lt.viko.eif.subtrack.entity.Subscription;
import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CurrencyServiceImpl}. All collaborators
 * ({@link SubscriptionRepository}, {@link CurrentUserProvider},
 * {@link SpendingMapper}, {@link RestTemplate}) are mocked with Mockito; no
 * Spring context, cache, database, or real HTTP call is involved. The
 * {@code @Value}-injected API URL/key are populated via reflection.
 */
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private SpendingMapper spendingMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    private User currentUser;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        // @Value fields are not populated by @InjectMocks; set them via reflection.
        ReflectionTestUtils.setField(currencyService, "apiUrl", "https://api.example.com/");
        ReflectionTestUtils.setField(currencyService, "apiKey", "test-key");

        currentUser = new User("alice", "alice@example.com", "hashed");
        currentUser.setId(1L);

        Category category = new Category("Streaming", "Streaming services");
        category.setId(5L);

        subscription = new Subscription(
                "Netflix", "Standard plan", new BigDecimal("10.00"), "EUR",
                BillingCycle.MONTHLY, LocalDate.of(2026, 7, 1), true);
        subscription.setId(10L);
        subscription.setUser(currentUser);
        subscription.setCategory(category);
    }

    /** Builds an ExchangeRate-API-shaped response with the given target rate. */
    private Map<String, Object> ratesResponse(String targetCode, Object rate) {
        return Map.of("conversion_rates", Map.of(targetCode, rate));
    }

    @Nested
    class SuccessfulConversion {

        @Test
        void convert_Success_AppliesRateAndReturnsResponse() {
            // Arrange
            CurrencyConversionResponse expected = new CurrencyConversionResponse(
                    new BigDecimal("10.00"), "EUR", new BigDecimal("11.00"),
                    "USD", new BigDecimal("1.1"));

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));
            when(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(ratesResponse("USD", 1.1));
            when(spendingMapper.toCurrencyConversionResponse(
                    eq(subscription), any(BigDecimal.class), eq("USD"), any(BigDecimal.class)))
                    .thenReturn(expected);

            // Act
            CurrencyConversionResponse result = currencyService.convert(10L, "USD");

            // Assert: returned response is the mapped one
            assertSame(expected, result);

            // Assert: conversion math is correct (10.00 * 1.1 = 11.00) and rate forwarded
            ArgumentCaptor<BigDecimal> convertedCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<BigDecimal> rateCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(spendingMapper).toCurrencyConversionResponse(
                    eq(subscription), convertedCaptor.capture(), eq("USD"), rateCaptor.capture());
            assertEquals(0, new BigDecimal("11.00").compareTo(convertedCaptor.getValue()));
            assertEquals(0, new BigDecimal("1.1").compareTo(rateCaptor.getValue()));
        }

        @Test
        void convert_LowercaseTarget_IsNormalisedToUpperCase() {
            // Arrange: caller passes "usd"; service must look up "USD"
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));
            when(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(ratesResponse("USD", 1.1));
            when(spendingMapper.toCurrencyConversionResponse(
                    eq(subscription), any(BigDecimal.class), eq("USD"), any(BigDecimal.class)))
                    .thenReturn(new CurrencyConversionResponse(
                            new BigDecimal("10.00"), "EUR", new BigDecimal("11.00"),
                            "USD", new BigDecimal("1.1")));

            // Act
            currencyService.convert(10L, "usd");

            // Assert: mapper was called with the upper-cased target
            verify(spendingMapper).toCurrencyConversionResponse(
                    eq(subscription), any(BigDecimal.class), eq("USD"), any(BigDecimal.class));
        }
    }

    @Nested
    class InvalidCurrency {

        @Test
        void convert_UnsupportedTargetCurrency_ThrowsIllegalArgument() {
            // Arrange: rates map does not contain the requested target
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));
            when(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(ratesResponse("USD", 1.1));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> currencyService.convert(10L, "XYZ"));
        }

        @Test
        void convert_MissingRatesInApiResponse_ThrowsIllegalArgument() {
            // Arrange: malformed API response (no conversion_rates key)
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));
            when(restTemplate.getForObject(anyString(), eq(Map.class)))
                    .thenReturn(Map.of("result", "error"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> currencyService.convert(10L, "USD"));
        }
    }

    @Nested
    class OwnershipAndNotFound {

        @Test
        void convert_SubscriptionNotFound_ThrowsNotFound() {
            // Arrange
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> currencyService.convert(99L, "USD"));
        }

        @Test
        void convert_SubscriptionOwnedByAnotherUser_ThrowsNotFound() {
            // Arrange: subscription belongs to user id 2, current user is id 1
            User otherUser = new User("bob", "bob@example.com", "hashed");
            otherUser.setId(2L);
            subscription.setUser(otherUser);

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));

            // Act & Assert: leaks nothing - reported as not found
            assertThrows(ResourceNotFoundException.class,
                    () -> currencyService.convert(10L, "USD"));
        }
    }
}