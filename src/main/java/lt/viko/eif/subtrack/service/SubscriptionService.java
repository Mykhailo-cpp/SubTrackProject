package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.SubscriptionRequest;
import lt.viko.eif.subtrack.dto.SubscriptionResponse;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Business operations for managing subscriptions.
 *
 * Every operation is implicitly scoped to the currently authenticated
 * user: listing returns only that user's subscriptions, and access to a
 * subscription owned by another user is treated as "not found" (HTTP 404) so
 * that the existence of other users' data is never revealed.
 */
public interface SubscriptionService {

    /**
     * Returns all subscriptions owned by the current user.
     *
     * @return the current user's subscriptions; empty if they have none
     */
    List<SubscriptionResponse> getAllForCurrentUser();

    /**
     * Returns a single subscription owned by the current user.
     *
     * @param id the subscription id
     * @return the matching subscription
     * @throws ResourceNotFoundException if no such subscription
     *         exists or it belongs to another user
     */
    SubscriptionResponse getByIdForCurrentUser(Long id);

    /**
     * Creates a subscription owned by the current user.
     *
     * @param request the subscription details
     * @return the created subscription, including its generated id
     * @throws ResourceNotFoundException if the referenced category does not exist
     */
    SubscriptionResponse create(SubscriptionRequest request);

    /**
     * Updates a subscription owned by the current user.
     *
     * @param id      the id of the subscription to update
     * @param request the new subscription details
     * @return the updated subscription
     * @throws ResourceNotFoundException if the subscription does not
     *         exist, belongs to another user, or the referenced category does not exist
     */
    SubscriptionResponse update(Long id, SubscriptionRequest request);

    /**
     * Deletes a subscription owned by the current user.
     *
     * @param id the id of the subscription to delete
     * @throws ResourceNotFoundException if no such subscription
     *         exists or it belongs to another user
     */
    void delete(Long id);
}