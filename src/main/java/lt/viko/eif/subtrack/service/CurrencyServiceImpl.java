package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.CurrencyConversionResponse;
import lt.viko.eif.subtrack.entity.Subscription;
import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;
import lt.viko.eif.subtrack.mapper.SpendingMapper;
import lt.viko.eif.subtrack.repository.SubscriptionRepository;
import lt.viko.eif.subtrack.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Default {@link CurrencyService} implementation.
 *
 * <p>Exchange rates are fetched from the ExchangeRate API using
 * {@link RestTemplate}. Results are cached by base currency for one hour
 * (cache name {@value #EXCHANGE_RATE_CACHE}) to limit external API calls.
 * The subscription must belong to the current user; ownership is enforced
 * by treating foreign subscriptions as not found. Price conversion is
 * calculated here; {@link SpendingMapper} assembles the response record.</p>
 */
@Service
@Transactional(readOnly = true)
public class CurrencyServiceImpl implements CurrencyService {

    /** Scale applied to converted monetary amounts. */
    private static final int MONETARY_SCALE = 2;

    /** Cache name used by Spring Cache for exchange-rate results. */
    public static final String EXCHANGE_RATE_CACHE = "exchangeRates";

    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SpendingMapper spendingMapper;
    private final RestTemplate restTemplate;

    @Value("${exchangerate.api.url}")
    private String apiUrl;

    @Value("${exchangerate.api.key}")
    private String apiKey;

    /**
     * Creates the service.
     *
     * @param subscriptionRepository the subscription repository
     * @param currentUserProvider    the current-user provider
     * @param spendingMapper         the spending mapper
     * @param restTemplate           the HTTP client for external API calls
     */
    public CurrencyServiceImpl(SubscriptionRepository subscriptionRepository,
                               CurrentUserProvider currentUserProvider,
                               SpendingMapper spendingMapper,
                               RestTemplate restTemplate) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserProvider = currentUserProvider;
        this.spendingMapper = spendingMapper;
        this.restTemplate = restTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public CurrencyConversionResponse convert(Long subscriptionId, String targetCurrency) {
        User currentUser = currentUserProvider.getCurrentUser();

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));

        if (!subscription.getUser().getId().equals(currentUser.getId())) {
            throw ResourceNotFoundException.of("Subscription", subscriptionId);
        }

        String normalizedTarget = targetCurrency.toUpperCase();
        BigDecimal rate = fetchRate(subscription.getCurrency(), normalizedTarget);
        BigDecimal converted = subscription.getPrice()
                .multiply(rate)
                .setScale(MONETARY_SCALE, RoundingMode.HALF_UP);

        return spendingMapper.toCurrencyConversionResponse(
                subscription, converted, normalizedTarget, rate);
    }

    /**
     * Fetches the exchange rate from {@code baseCurrency} to {@code targetCurrency}.
     *
     * <p>Results are cached under {@value #EXCHANGE_RATE_CACHE} keyed by base
     * currency. The full rates map for the base currency is cached so that
     * converting to multiple targets from the same base requires only one API call.</p>
     *
     * @param baseCurrency   the ISO 4217 source currency code
     * @param targetCurrency the ISO 4217 target currency code
     * @return the exchange rate as a {@link BigDecimal}
     * @throws IllegalArgumentException if the target currency is not supported
     */
    @Cacheable(value = EXCHANGE_RATE_CACHE, key = "#baseCurrency")
    public BigDecimal fetchRate(String baseCurrency, String targetCurrency) {
        String url = apiUrl + apiKey + "/latest/" + baseCurrency;

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || !response.containsKey("conversion_rates")) {
            throw new IllegalArgumentException(
                    "Failed to fetch exchange rates for currency: " + baseCurrency);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) response.get("conversion_rates");

        if (!rates.containsKey(targetCurrency)) {
            throw new IllegalArgumentException(
                    "Unsupported target currency: " + targetCurrency);
        }

        return new BigDecimal(rates.get(targetCurrency).toString());
    }
}
