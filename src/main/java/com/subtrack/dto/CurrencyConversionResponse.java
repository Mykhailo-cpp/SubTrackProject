package com.subtrack.dto;

import java.math.BigDecimal;

/**
 * Response payload for a currency conversion result.
 *
 * <p>Returns both the original price and the converted amount so the client
 * can present both values to the user. Assembled by
 * {@link com.subtrack.mapper.SpendingMapper}.</p>
 *
 * @param originalPrice    the original price as stored on the subscription
 * @param originalCurrency ISO 4217 code of the original currency
 * @param convertedPrice   price converted to the target currency
 * @param targetCurrency   ISO 4217 code of the target currency
 * @param exchangeRate     the exchange rate applied during conversion
 */
public record CurrencyConversionResponse(
        BigDecimal originalPrice,
        String originalCurrency,
        BigDecimal convertedPrice,
        String targetCurrency,
        BigDecimal exchangeRate
) {}
