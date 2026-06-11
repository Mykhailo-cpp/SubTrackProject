package lt.viko.eif.subtrack.repository;

import lt.viko.eif.subtrack.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Subscription} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}
 * plus queries used by the service layer to scope subscriptions to a user,
 * filter active ones, and surface upcoming renewals.</p>
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Finds all subscriptions belonging to the given user.
     *
     * @param userId the id of the owning user
     * @return the user's subscriptions; an empty list if they have none
     */
    List<Subscription> findByUserId(Long userId);

    /**
     * Finds all active subscriptions belonging to the given user.
     *
     * @param userId the id of the owning user
     * @return the user's active subscriptions; an empty list if they have none
     */
    List<Subscription> findByUserIdAndActiveTrue(Long userId);

    /**
     * Finds subscriptions whose next renewal date falls before the given date.
     *
     * <p>Used to surface renewals that are due soon (for example, by passing
     * {@code LocalDate.now().plusDays(7)} to find renewals within the next week).</p>
     *
     * @param date the exclusive upper bound for the next renewal date
     * @return subscriptions renewing before {@code date}; an empty list if none
     */
    List<Subscription> findByNextRenewalDateBefore(LocalDate date);

    /**
     * Finds subscriptions belonging to the given user within a specific category.
     *
     * @param userId     the id of the owning user
     * @param categoryId the id of the category to filter by
     * @return the matching subscriptions; an empty list if none
     */
    List<Subscription> findByUserIdAndCategoryId(Long userId, Long categoryId);
}