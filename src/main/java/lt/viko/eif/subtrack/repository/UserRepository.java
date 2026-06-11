package lt.viko.eif.subtrack.repository;

import lt.viko.eif.subtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}
 * plus lookups and existence checks used during authentication and
 * registration.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the matching user, or empty if none exists
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their unique email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the matching user, or empty if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a user with that username exists, otherwise {@code false}
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether a user with the given email address already exists.
     *
     * @param email the email address to check
     * @return {@code true} if a user with that email exists, otherwise {@code false}
     */
    boolean existsByEmail(String email);
}