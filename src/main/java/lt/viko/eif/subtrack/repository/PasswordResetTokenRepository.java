package lt.viko.eif.subtrack.repository;

import lt.viko.eif.subtrack.entity.PasswordResetToken;
import lt.viko.eif.subtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data repository for {@link PasswordResetToken}.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Looks up a token by its value — used during the reset step. */
    Optional<PasswordResetToken> findByToken(String token);

    /** Looks up a token by its owner — used to overwrite an existing token. */
    Optional<PasswordResetToken> findByUser(User user);

    /** Deletes any existing token for the given user. */
    void deleteByUser(User user);
}
