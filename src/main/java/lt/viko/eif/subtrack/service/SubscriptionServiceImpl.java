package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.SubscriptionRequest;
import lt.viko.eif.subtrack.dto.SubscriptionResponse;
import lt.viko.eif.subtrack.entity.Category;
import lt.viko.eif.subtrack.entity.Subscription;
import lt.viko.eif.subtrack.entity.User;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;
import lt.viko.eif.subtrack.mapper.SubscriptionMapper;
import lt.viko.eif.subtrack.repository.CategoryRepository;
import lt.viko.eif.subtrack.repository.SubscriptionRepository;
import lt.viko.eif.subtrack.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default {@link SubscriptionService} implementation.
 *
 * All operations are scoped to the user supplied by
 * {@link CurrentUserProvider}. Ownership is enforced by treating any
 * subscription that does not belong to the current user as not found, which
 * yields HTTP 404 and avoids disclosing the existence of other users'
 * records. Entity/DTO conversion is delegated to {@link SubscriptionMapper};
 * the owning user and category are resolved here and set on the entity after
 * mapping. Mapping runs inside the transactional boundary so the lazily-loaded
 * category can be read safely.
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

    /** Mapper converting between subscription entities and DTOs. */
    private final SubscriptionMapper subscriptionMapper;

    /**
     * Creates the service with its required collaborators.
     *
     * @param subscriptionRepository the subscription repository
     * @param categoryRepository     the category repository
     * @param currentUserProvider    the current-user provider
     * @param subscriptionMapper     the subscription mapper
     */
    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                   CategoryRepository categoryRepository,
                                   CurrentUserProvider currentUserProvider,
                                   SubscriptionMapper subscriptionMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserProvider = currentUserProvider;
        this.subscriptionMapper = subscriptionMapper;
    }

    /** {@inheritDoc} */
    @Override
    public List<SubscriptionResponse> getAllForCurrentUser() {
        User currentUser = currentUserProvider.getCurrentUser();
        return subscriptionMapper.toResponseList(
                subscriptionRepository.findByUserId(currentUser.getId()));
    }

    /** {@inheritDoc} */
    @Override
    public SubscriptionResponse getByIdForCurrentUser(Long id) {
        return subscriptionMapper.toResponse(findOwnedSubscriptionOrThrow(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        User currentUser = currentUserProvider.getCurrentUser();
        Category category = findCategoryOrThrow(request.getCategoryId());

        Subscription subscription = subscriptionMapper.toEntity(request);
        subscription.setUser(currentUser);
        subscription.setCategory(category);

        Subscription saved = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponse(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        Subscription subscription = findOwnedSubscriptionOrThrow(id);
        Category category = findCategoryOrThrow(request.getCategoryId());

        subscriptionMapper.updateEntity(request, subscription);
        subscription.setCategory(category);

        Subscription saved = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponse(saved);
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
}