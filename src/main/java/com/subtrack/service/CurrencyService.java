package com.subtrack.service;

import com.subtrack.dto.CurrencyConversionResponse;

/**
 * Service for converting subscription prices to a target currency.
 */
public interface CurrencyService {
    /**
     * Converts the price of the given subscription to the requested currency.
     *
     * <p>The subscription must belong to the currently authenticated user;
     * a subscription that does not exist or belongs to another user is treated
     * as not found. The original price and currency are preserved in the
     * response alongside the converted amount and the exchange rate applied.</p>
     *
     * @param subscriptionId the id of the subscription to convert
     * @param targetCurrency the ISO 4217 target currency code (e.g. {@code "USD"})
     * @return the conversion result containing both the original and converted price
     * @throws com.subtrack.exception.ResourceNotFoundException if the subscription
     *         does not exist or belongs to another user
     * @throws IllegalArgumentException if the target currency code is unsupported
     */
    CurrencyConversionResponse convert(Long subscriptionId, String targetCurrency);
}