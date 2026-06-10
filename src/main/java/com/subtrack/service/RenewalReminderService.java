package com.subtrack.service;

/**
 * Service responsible for detecting upcoming subscription renewals and
 * dispatching reminder emails to the affected users.
 */
public interface RenewalReminderService {

    /**
     * Scans all active subscriptions and sends a reminder email for every
     * subscription whose {@code nextRenewalDate} falls within the configured
     * look-ahead window (default: 7 days).
     *
     * <p>This method is intended to be called by the scheduled job in
     * {@link com.subtrack.scheduler.RenewalReminderScheduler} and should not
     * be invoked directly in production code.</p>
     */
    void sendUpcomingRenewalReminders();
}
