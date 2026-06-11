package lt.viko.eif.subtrack.service;

/**
 * Service for sending transactional emails to application users.
 *
 * <p>Implementations are responsible for constructing and delivering the
 * message via the configured mail transport. Callers need not know anything
 * about the underlying mail provider.</p>
 */
public interface EmailService {

    /**
     * Sends a plain-text email to the given recipient.
     *
     * @param to      the recipient email address
     * @param subject the email subject line
     * @param body    the plain-text email body
     */
    void sendEmail(String to, String subject, String body);
}
