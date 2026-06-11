package lt.viko.eif.subtrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for the forgot-password endpoint.
 *
 * <p>The client supplies only the email address. The backend locates the
 * matching account (if any) and sends a reset link. If no account exists for
 * the given email, the endpoint returns 200 anyway — this prevents user
 * enumeration attacks where an attacker could discover which emails are
 * registered by observing different responses.</p>
 */
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
