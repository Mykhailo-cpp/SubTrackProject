package lt.viko.eif.subtrack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Default {@link EmailService} implementation backed by Spring's
 * {@link JavaMailSender}.
 *
 * <p>Sends plain-text emails via the SMTP host configured in
 * {@code application.properties}. If the mail transport throws, the
 * exception is logged and swallowed so that a delivery failure never
 * disrupts the calling thread (typically a scheduler).</p>
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    /** The configured from-address for all outgoing emails. */
    @Value("${spring.mail.username}")
    private String fromAddress;

    /** Spring mail sender backed by the configured SMTP host. */
    private final JavaMailSender mailSender;

    /**
     * Creates the service.
     *
     * @param mailSender the Spring mail sender
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delivery failures are logged at ERROR level and silently swallowed
     * so that a transient SMTP error does not abort the reminder job.</p>
     */
    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Reminder email sent to {}", to);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
