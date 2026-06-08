package com.subtrack.dto;

import com.subtrack.entity.BillingCycle;
import com.subtrack.entity.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response payload representing a subscription returned to the client.
 *
 * Exposes presentational fields plus a flattened reference to the owning
 * category ({@code categoryId}/{@code categoryName}). The owning user is
 * deliberately omitted: subscriptions are always returned in the context of
 * the authenticated user, so echoing the user back adds nothing and avoids a
 * serialisation cycle. HATEOAS links are attached by the controller layer via
 * an {@code EntityModel} wrapper rather than being held on this DTO.
 */
public class SubscriptionResponse {

    /** The subscription's unique identifier. */
    private Long id;

    /** The service name. */
    private String name;

    /** The description, possibly {@code null}. */
    private String description;

    /** The recurring price. */
    private BigDecimal price;

    /** The ISO 4217 currency code. */
    private String currency;

    /** The billing frequency. */
    private BillingCycle billingCycle;

    /** The next renewal date. */
    private LocalDate nextRenewalDate;

    /** Whether the subscription is active. */
    private boolean active;

    /** Id of the owning category. */
    private Long categoryId;

    /** Name of the owning category. */
    private String categoryName;

    /** Default constructor. */
    public SubscriptionResponse() {
    }

    /**
     * Maps a {@link Subscription} entity to a response DTO.
     *
     * Must be invoked within an active persistence context, since it reads
     * the lazily-loaded category.
     *
     * @param s the entity to convert
     * @return a populated {@link SubscriptionResponse}
     */
    public static SubscriptionResponse fromEntity(Subscription s) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.id = s.getId();
        r.name = s.getName();
        r.description = s.getDescription();
        r.price = s.getPrice();
        r.currency = s.getCurrency();
        r.billingCycle = s.getBillingCycle();
        r.nextRenewalDate = s.getNextRenewalDate();
        r.active = s.isActive();
        if (s.getCategory() != null) {
            r.categoryId = s.getCategory().getId();
            r.categoryName = s.getCategory().getName();
        }
        return r;
    }

    /** @return the id */
    public Long getId() {
        return id;
    }

    /** @param id the id */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @param name the name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the price */
    public BigDecimal getPrice() {
        return price;
    }

    /** @param price the price */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /** @return the currency code */
    public String getCurrency() {
        return currency;
    }

    /** @param currency the currency code */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /** @return the billing cycle */
    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    /** @param billingCycle the billing cycle */
    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    /** @return the next renewal date */
    public LocalDate getNextRenewalDate() {
        return nextRenewalDate;
    }

    /** @param nextRenewalDate the next renewal date */
    public void setNextRenewalDate(LocalDate nextRenewalDate) {
        this.nextRenewalDate = nextRenewalDate;
    }

    /** @return whether active */
    public boolean isActive() {
        return active;
    }

    /** @param active the active flag */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** @return the category id */
    public Long getCategoryId() {
        return categoryId;
    }

    /** @param categoryId the category id */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /** @return the category name */
    public String getCategoryName() {
        return categoryName;
    }

    /** @param categoryName the category name */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}