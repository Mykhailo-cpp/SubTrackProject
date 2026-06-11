package lt.viko.eif.subtrack.dto;

import lt.viko.eif.subtrack.entity.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for creating or updating a subscription.
 *
 * The owning user is never supplied by the client; it is taken from the
 * authenticated principal. The category is referenced by id and resolved
 * server-side.
 */
public class SubscriptionRequest {

    /** Display name of the subscribed service. Must not be blank. */
    @NotBlank(message = "Subscription name is required")
    @Size(max = 100, message = "Subscription name must not exceed 100 characters")
    private String name;

    /** Optional free-text description. */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /** Recurring price per billing cycle. Must be a positive amount. */
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    /** ISO 4217 currency code (for example, "EUR"). */
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code")
    private String currency;

    /** Billing frequency. */
    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    /** Date the subscription next renews. */
    @NotNull(message = "Next renewal date is required")
    private LocalDate nextRenewalDate;

    /** Whether the subscription is active. Defaults to {@code true} when omitted. */
    private Boolean active;

    /**
     * Whether the user wants an email reminder before this subscription renews.
     * Defaults to {@code false} (opt-in). When omitted on update, the stored
     * value is preserved by MapStruct's null-guard.
     */
    private Boolean renewalReminderEnabled;

    /** Id of the category this subscription belongs to. */
    @NotNull(message = "Category id is required")
    private Long categoryId;

    /** Default constructor required for JSON deserialisation. */
    public SubscriptionRequest() {
    }

    /**
     * Returns the service name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the service name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description.
     *
     * @return the description, possibly {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the price.
     *
     * @return the price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Sets the price.
     *
     * @param price the price
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Returns the currency code.
     *
     * @return the currency code
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets the currency code.
     *
     * @param currency the currency code
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Returns the billing cycle.
     *
     * @return the billing cycle
     */
    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    /**
     * Sets the billing cycle.
     *
     * @param billingCycle the billing cycle
     */
    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    /**
     * Returns the next renewal date.
     *
     * @return the next renewal date
     */
    public LocalDate getNextRenewalDate() {
        return nextRenewalDate;
    }

    /**
     * Sets the next renewal date.
     *
     * @param nextRenewalDate the next renewal date
     */
    public void setNextRenewalDate(LocalDate nextRenewalDate) {
        this.nextRenewalDate = nextRenewalDate;
    }

    /**
     * Returns the active flag.
     *
     * @return the active flag, possibly {@code null}
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the active flag.
     *
     * @param active the active flag
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Returns the renewal reminder flag.
     *
     * @return the renewal reminder flag, possibly {@code null}
     */
    public Boolean getRenewalReminderEnabled() {
        return renewalReminderEnabled;
    }

    /**
     * Sets the renewal reminder flag.
     *
     * @param renewalReminderEnabled the renewal reminder flag
     */
    public void setRenewalReminderEnabled(Boolean renewalReminderEnabled) {
        this.renewalReminderEnabled = renewalReminderEnabled;
    }

    /**
     * Returns the category id.
     *
     * @return the category id
     */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * Sets the category id.
     *
     * @param categoryId the category id
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}