package lt.viko.eif.subtrack.dto;

import lt.viko.eif.subtrack.entity.BillingCycle;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response payload representing a subscription returned to the client.
 *
 * <p>The owning user is deliberately omitted: subscriptions are always
 * returned in the context of the authenticated user, so echoing the user
 * back adds nothing and avoids a serialisation cycle. The category is
 * flattened to {@code categoryId}/{@code categoryName}. HATEOAS links are
 * attached by the controller layer via an {@code EntityModel} wrapper rather
 * than being held on this record. Mapping from the entity is performed by
 * {@code com.subtrack.mapper.SubscriptionMapper}.</p>
 *
 * @param id                     the subscription's unique identifier
 * @param name                   the service name
 * @param description            the optional description, possibly {@code null}
 * @param price                  the recurring price
 * @param currency               the ISO 4217 currency code
 * @param billingCycle           the billing frequency
 * @param nextRenewalDate        the next renewal date
 * @param active                 whether the subscription is currently active
 * @param renewalReminderEnabled whether the user wants an email reminder before renewal
 * @param categoryId             the id of the owning category
 * @param categoryName           the name of the owning category
 */
public record SubscriptionResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        BillingCycle billingCycle,
        LocalDate nextRenewalDate,
        boolean active,
        boolean renewalReminderEnabled,
        Long categoryId,
        String categoryName
) {}