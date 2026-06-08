package com.subtrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource cannot be found.
 *
 * <p>Annotated with {@link ResponseStatus} so that, absent a more specific
 * handler, Spring MVC responds with HTTP 404 (Not Found).</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message detail describing which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience factory producing a uniformly formatted message.
     *
     * @param resource the resource type (for example, "Category")
     * @param id       the identifier that was not found
     * @return a new {@link ResourceNotFoundException}
     */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " not found with id: " + id);
    }
}