package lt.viko.eif.subtrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A single-use token that allows a user to reset their password without
 * knowing their current credentials.
 *
 * <p>The token is a cryptographically random UUID generated at request time,
 * stored here alongside its expiry. Once used — or once expired — it must
 * be deleted so it cannot be replayed.</p>
 *
 * <p>The relationship to {@link User} is {@code OneToOne}: at most one active
 * reset token exists per user at any time. Requesting a new token while one
 * already exists overwrites it, invalidating the earlier email.</p>
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The opaque reset token sent to the user's email address.
     * Stored as plain text because it is a one-time random value, not
     * a secret that must be protected at rest the way a password is.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /** The user this token belongs to. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The point in time after which this token is no longer valid.
     * Tokens are valid for 1 hour from creation.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Default no-args constructor required by JPA. */
    public PasswordResetToken() {}

    /**
     * Creates a reset token for the given user.
     *
     * @param token     the random token string
     * @param user      the owning user
     * @param expiresAt the expiry timestamp
     */
    public PasswordResetToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    /** Returns whether this token has passed its expiry time. */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public Long getId() { return id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
