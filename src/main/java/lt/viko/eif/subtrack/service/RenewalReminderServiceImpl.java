package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.entity.Subscription;
import lt.viko.eif.subtrack.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Default {@link RenewalReminderService} implementation.
 *
 * <p>Fetches all active subscriptions whose {@code nextRenewalDate} falls
 * within the next {@value #LOOK_AHEAD_DAYS} days, then delegates email
 * delivery to {@link EmailService}. Each reminder email includes the
 * subscription name, price, currency, and renewal date so the user has
 * everything they need at a glance.</p>
 *
 * <p>Subscriptions whose owner has no email address on record are silently
 * skipped — this should not occur in practice because email is required at
 * registration, but defensive handling prevents a {@code NullPointerException}
 * from aborting the entire batch.</p>
 */
@Service
@Transactional(readOnly = true)
public class RenewalReminderServiceImpl implements RenewalReminderService {

    private static final Logger log = LoggerFactory.getLogger(RenewalReminderServiceImpl.class);

    /** Number of days ahead to look for upcoming renewals. */
    private static final int LOOK_AHEAD_DAYS = 7;

    /** Email subject template. The subscription name is appended at runtime. */
    private static final String SUBJECT_TEMPLATE = "SubTrack — Renewal reminder: ";

    /** Repository used to query subscriptions due for renewal. */
    private final SubscriptionRepository subscriptionRepository;

    /** Service used to send the reminder emails. */
    private final EmailService emailService;

    /**
     * Creates the service.
     *
     * @param subscriptionRepository the subscription repository
     * @param emailService           the email service
     */
    public RenewalReminderServiceImpl(SubscriptionRepository subscriptionRepository,
                                      EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries subscriptions with a {@code nextRenewalDate} between today
     * (inclusive) and today plus {@value #LOOK_AHEAD_DAYS} days (inclusive).
     * A reminder email is sent for each result whose owner has a non-blank
     * email address.</p>
     */
    @Override
    public void sendUpcomingRenewalReminders() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(LOOK_AHEAD_DAYS);

        List<Subscription> upcoming =
                subscriptionRepository.findByNextRenewalDateBefore(cutoff)
                        .stream()
                        .filter(s -> s.isActive() && !s.getNextRenewalDate().isBefore(today))
                        .toList();

        log.info("Found {} subscription(s) renewing between {} and {}", upcoming.size(), today, cutoff);

        for (Subscription subscription : upcoming) {
            String email = subscription.getUser().getEmail();

            if (!subscription.isRenewalReminderEnabled()) {
                log.debug("Skipping reminder for subscription id={} — renewal reminders disabled",
                        subscription.getId());
                continue;
            }

            if (email == null || email.isBlank()) {
                log.warn("Skipping reminder for subscription id={} — user has no email address",
                        subscription.getId());
                continue;
            }

            emailService.sendEmail(
                    email,
                    SUBJECT_TEMPLATE + subscription.getName(),
                    buildEmailBody(subscription));
        }
    }

    /**
     * Constructs the plain-text body for a renewal reminder email.
     *
     * @param subscription the subscription that is about to renew
     * @return the formatted email body
     */
    private String buildEmailBody(Subscription subscription) {
        return String.format(
                "Hi %s,%n%n" +
                        "This is a reminder that your subscription is renewing soon.%n%n" +
                        "  Service:       %s%n" +
                        "  Price:         %s %s%n" +
                        "  Renewal date:  %s%n%n" +
                        "Log in to SubTrack to manage your subscriptions.%n%n" +
                        "— The SubTrack Team",
                subscription.getUser().getUsername(),
                subscription.getName(),
                subscription.getPrice().toPlainString(),
                subscription.getCurrency(),
                subscription.getNextRenewalDate()
        );
    }
}