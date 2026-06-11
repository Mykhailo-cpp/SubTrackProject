package lt.viko.eif.subtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for the reset-password endpoint.
 *
 * <p>The client supplies the token received by email and the desired new
 * password. The same password rules that apply at registration are enforced
 * here so users cannot reset to a weak password.</p>
 */
public class ResetPasswordRequest {

    /** The opaque token from the reset email. */
    @NotBlank(message = "Token is required")
    private String token;

    /** The new password the user wants to set. */
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
            message = "Password must contain at least one special character"
    )
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
