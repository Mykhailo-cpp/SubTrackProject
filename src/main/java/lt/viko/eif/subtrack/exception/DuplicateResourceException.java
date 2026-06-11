package lt.viko.eif.subtrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when creating or renaming a resource would violate a uniqueness
 * constraint (for example, a category name that already exists).
 *
 * <p>Annotated with {@link ResponseStatus} so that, absent a more specific
 * handler, Spring MVC responds with HTTP 409 (Conflict).</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message detail describing the conflicting value
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}