package lt.viko.eif.subtrack.scheduler;

import lt.viko.eif.subtrack.service.RenewalReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers daily renewal reminder emails.
 *
 * <p>Runs every day at 09:00 (server time) using a cron expression.
 * Actual business logic is fully delegated to {@link RenewalReminderService}
 * so this class remains a thin orchestration layer with no direct
 * dependencies on repositories or mail infrastructure.</p>
 *
 * <p>Scheduling must be enabled globally via {@code @EnableScheduling} on
 * the main application class for this job to fire.</p>
 */
@Component
public class RenewalReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenewalReminderScheduler.class);

    /** Service that performs the actual reminder logic. */
    private final RenewalReminderService renewalReminderService;

    /**
     * Creates the scheduler.
     *
     * @param renewalReminderService the renewal reminder service
     */
    public RenewalReminderScheduler(RenewalReminderService renewalReminderService) {
        this.renewalReminderService = renewalReminderService;
    }

    /**
     * Triggers the renewal reminder job.
     *
     * <p>Scheduled to run daily at 09:00 server time. The cron expression
     * {@code "0 0 9 * * *"} means: at second 0, minute 0, hour 9, every
     * day of the month, every month, every day of the week.</p>
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void runDailyRenewalReminders() {
        log.info("Running daily renewal reminder job");
        renewalReminderService.sendUpcomingRenewalReminders();
        log.info("Daily renewal reminder job completed");
    }
}