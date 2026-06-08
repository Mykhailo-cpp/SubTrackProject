package com.subtrack.service;

import com.subtrack.dto.SubscriptionRequest;
import com.subtrack.dto.SubscriptionResponse;
import com.subtrack.entity.Category;
import com.subtrack.entity.Subscription;
import com.subtrack.entity.User;
import com.subtrack.exception.ResourceNotFoundException;
import com.subtrack.repository.CategoryRepository;
import com.subtrack.repository.SubscriptionRepository;
import com.subtrack.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default {@link SubscriptionService} implementation.
 * All operations are scoped to the user supplied by
 * {@link CurrentUserProvider}. Ownership is enforced by treating any
 * subscription that does not belong to the current user as not found, which
 * yields HTTP 404 and avoids disclosing the existence of other users'
 * records. Entity-to-DTO mapping happens inside the transactional boundary so
 * that the lazily-loaded category can be read safely.
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    /** Repository for subscription persistence. */
    private final SubscriptionRepository subscriptionRepository;

    /** Repository used to resolve referenced categories. */
    private final CategoryRepository categoryRepository;

    /** Supplies the currently authenticated user. */
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the service with its required collaborators.
     *
     * @param subscriptionRepository the subscription repository
     * @param categoryRepository     the category repository
     * @param currentUserProvider    the current-user provider
     */
    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   CategoryRepository categoryRepository,
                                   CurrentUserProvider currentUserProvider) {
        this.subscriptionRepository = subscriptionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /** {@inheritDoc} */
    @Override
    public List<SubscriptionResponse> getAllForCurrentUser() {
        User currentUser = currentUserProvider.getCurrentUser();
        return subscriptionRepository.findByUserId(currentUser.getId()).stream()
                .map(SubscriptionResponse::fromEntity)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public SubscriptionResponse getByIdForCurrentUser(Long id) {
        Subscription subscription = findOwnedSubscriptionOrThrow(id);
        return SubscriptionResponse.fromEntity(subscription);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();
        Category category = findCategoryOrThrow(request.getCategoryId());

        Subscription subscription = new Subscription();
        applyRequest(subscription, request, category);
        subscription.setUser(currentUser);

        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        Subscription subscription = findOwnedSubscriptionOrThrow(id);
        Category category = findCategoryOrThrow(request.getCategoryId());

        applyRequest(subscription, request, category);

        Subscription saved = subscriptionRepository.save(subscription);
        return SubscriptionResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long id) {
        Subscription subscription = findOwnedSubscriptionOrThrow(id);
        subscriptionRepository.delete(subscription);
    }

    /**
     * Loads a subscription by id, verifying it belongs to the current user.
     *
     * @param id the subscription id
     * @return the owned subscription
     * @throws ResourceNotFoundException if the subscription is missing or owned by another user
     */
    private Subscription findOwnedSubscriptionOrThrow(Long id) {
        User currentUser = currentUserProvider.getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", id));
        // Treat another user's subscription as non-existent rather than forbidden,
        // so the response does not leak that the id is in use.
        if (!subscription.getUser().getId().equals(currentUser.getId())) {
            throw ResourceNotFoundException.of("Subscription", id);
        }
        return subscription;
    }

    /**
     * Loads a category by id or throws if it does not exist.
     *
     * @param categoryId the category id
     * @return the category
     * @throws ResourceNotFoundException if no category has the given id
     */
    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
    }

    /**
     * Copies request fields onto a subscription, defaulting {@code active} to
     * {@code true} when the client omits it.
     *
     * @param subscription the target entity
     * @param request      the source request
     * @param category     the resolved category to associate
     */
    private void applyRequest(Subscription subscription, SubscriptionRequest request, Category category) {
        subscription.setName(request.getName());
        subscription.setDescription(request.getDescription());
        subscription.setPrice(request.getPrice());
        subscription.setCurrency(request.getCurrency());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setNextRenewalDate(request.getNextRenewalDate());
        subscription.setActive(request.getActive() == null || request.getActive());
        subscription.setCategory(category);
    }
}