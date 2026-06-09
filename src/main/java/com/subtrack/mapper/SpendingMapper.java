package com.subtrack.mapper;

import com.subtrack.dto.SpendingSummaryResponse.CategorySummary;
import com.subtrack.dto.SpendingSummaryResponse.SubscriptionSummary;
import com.subtrack.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapStruct mapper that assembles spending-related record DTOs from
 * pre-calculated domain data.
 *
 * <p>All price calculations and billing-cycle normalisation are performed
 * in the service layer before being passed here. This mapper is responsible
 * only for binding the computed values into the correct record fields —
 * keeping the mapping layer free of business logic.</p>
 */
@Mapper(componentModel = "spring")
public interface SpendingMapper {

    /**
     * Assembles a {@link SubscriptionSummary} from a subscription entity and
     * its pre-calculated monthly and yearly costs.
     *
     * @param subscription the subscription entity supplying name, price and currency
     * @param monthly      the normalised monthly cost, calculated by the service
     * @param yearly       the normalised yearly cost, calculated by the service
     * @return the assembled subscription summary record
     */
    @Mapping(target = "name", source = "subscription.name")
    @Mapping(target = "originalPrice", source = "subscription.price")
    @Mapping(target = "currency", source = "subscription.currency")
    @Mapping(target = "monthlyPrice", source = "monthly")
    @Mapping(target = "yearlyPrice", source = "yearly")
    SubscriptionSummary toSubscriptionSummary(
            Subscription subscription,
            BigDecimal monthly,
            BigDecimal yearly);

    /**
     * Assembles a {@link CategorySummary} from a category name and its
     * already-mapped subscription summaries.
     *
     * @param categoryName  the name of the category
     * @param subscriptions the pre-mapped subscription summaries
     * @return the assembled category summary record
     */
    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "subscriptions", source = "subscriptions")
    CategorySummary toCategorySummary(
            String categoryName,
            List<SubscriptionSummary> subscriptions);
}
