package com.subtrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised exception-to-HTTP-status mapping for SubTrack.
 *
 * <p>Spring MVC's {@link org.springframework.web.bind.annotation.ResponseStatus}
 * already handles {@link ResourceNotFoundException} (404) and
 * {@link DuplicateResourceException} (409) when they are thrown from the
 * service layer.  This advice handles the remaining cases that are not
 * covered by those annotations:</p>
 *
 * <ul>
 *   <li>{@link IllegalArgumentException} — thrown by
 *       {@link com.subtrack.service.AuthServiceImpl} for duplicate username /
 *       email during registration → HTTP 409 Conflict.</li>
 *   <li>{@link BadCredentialsException} — thrown by Spring Security's
 *       {@code AuthenticationManager} for wrong password → HTTP 401
 *       Unauthorized.</li>
 * </ul>
 *
 * <p>Responses use RFC 9457 {@link ProblemDetail} so clients always receive a
 * structured JSON body with {@code status} and {@code detail} fields.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maps {@link IllegalArgumentException} to 409 Conflict.
     *
     * <p>Used when registration is attempted with an already-taken username
     * or e-mail address.</p>
     *
     * @param ex the exception
     * @return a {@link ProblemDetail} with status 409
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setDetail(ex.getMessage());
        return pd;
    }

    /**
     * Maps {@link BadCredentialsException} to 401 Unauthorized.
     *
     * <p>Spring Security throws this when the supplied password does not match
     * the stored hash during login.</p>
     *
     * @param ex the exception
     * @return a {@link ProblemDetail} with status 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setDetail("Invalid username or password");
        return pd;
    }
}
