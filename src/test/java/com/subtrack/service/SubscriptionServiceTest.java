package com.subtrack.service;

import com.subtrack.dto.SubscriptionRequest;
import com.subtrack.dto.SubscriptionResponse;
import com.subtrack.entity.BillingCycle;
import com.subtrack.entity.Category;
import com.subtrack.entity.Subscription;
import com.subtrack.entity.User;
import com.subtrack.exception.ResourceNotFoundException;
import com.subtrack.mapper.SubscriptionMapper;
import com.subtrack.repository.CategoryRepository;
import com.subtrack.repository.SubscriptionRepository;
import com.subtrack.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubscriptionServiceImpl}. All collaborators
 * ({@link SubscriptionRepository}, {@link CategoryRepository},
 * {@link CurrentUserProvider}, {@link SubscriptionMapper}) are mocked with
 * Mockito; no Spring context or database is involved. Particular attention is
 * paid to ownership scoping: another user's subscription must be reported as
 * not found.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User currentUser;
    private Category category;
    private SubscriptionRequest request;
    private SubscriptionResponse response;

    @BeforeEach
    void setUp() {
        currentUser = new User("alice", "alice@example.com", "hashed");
        currentUser.setId(1L);

        category = new Category("Streaming", "Streaming services");
        category.setId(5L);

        request = new SubscriptionRequest();
        request.setName("Netflix");
        request.setDescription("Standard plan");
        request.setPrice(new BigDecimal("9.99"));
        request.setCurrency("EUR");
        request.setBillingCycle(BillingCycle.MONTHLY);
        request.setNextRenewalDate(LocalDate.of(2026, 7, 1));
        request.setActive(true);
        request.setCategoryId(5L);

        response = new SubscriptionResponse();
        response.setId(10L);
        response.setName("Netflix");
    }

    /** Builds a subscription owned by the given user. */
    private Subscription subscriptionOwnedBy(User owner) {
        Subscription s = new Subscription(
                "Netflix", "Standard plan", new BigDecimal("9.99"), "EUR",
                BillingCycle.MONTHLY, LocalDate.of(2026, 7, 1), true);
        s.setId(10L);
        s.setUser(owner);
        s.setCategory(category);
        return s;
    }

    @Nested
    class CreateTests {

        @Test
        void create_Success_LinksUserAndCategory() {
            // Arrange
            Subscription mapped = new Subscription();
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(subscriptionMapper.toEntity(request)).thenReturn(mapped);
            when(subscriptionRepository.save(mapped)).thenReturn(mapped);
            when(subscriptionMapper.toResponse(mapped)).thenReturn(response);

            // Act
            SubscriptionResponse result = subscriptionService.create(request);

            // Assert
            assertSame(response, result);
            assertSame(currentUser, mapped.getUser());
            assertSame(category, mapped.getCategory());
            verify(subscriptionRepository).save(mapped);
        }

        @Test
        void create_CategoryNotFound_Throws() {
            // Arrange
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> subscriptionService.create(request));
            verify(subscriptionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    class GetTests {

        @Test
        void getById_OwnedByCurrentUser_ReturnsResponse() {
            // Arrange
            Subscription owned = subscriptionOwnedBy(currentUser);
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(owned));
            when(subscriptionMapper.toResponse(owned)).thenReturn(response);

            // Act
            SubscriptionResponse result = subscriptionService.getByIdForCurrentUser(10L);

            // Assert
            assertSame(response, result);
        }

        @Test
        void getById_NotFound_Throws() {
            // Arrange
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> subscriptionService.getByIdForCurrentUser(99L));
        }

        @Test
        void getById_OwnedByAnotherUser_ThrowsNotFound() {
            // Arrange: subscription belongs to user id 2, current user is id 1
            User otherUser = new User("bob", "bob@example.com", "hashed");
            otherUser.setId(2L);
            Subscription othersSubscription = subscriptionOwnedBy(otherUser);

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(othersSubscription));

            // Act & Assert: leaks nothing — reported as not found, not forbidden
            assertThrows(ResourceNotFoundException.class,
                    () -> subscriptionService.getByIdForCurrentUser(10L));
        }

        @Test
        void getAllForCurrentUser_ReturnsMappedList() {
            // Arrange
            List<Subscription> entities = List.of(subscriptionOwnedBy(currentUser));
            List<SubscriptionResponse> responses = List.of(response);
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findByUserId(1L)).thenReturn(entities);
            when(subscriptionMapper.toResponseList(entities)).thenReturn(responses);

            // Act
            List<SubscriptionResponse> result = subscriptionService.getAllForCurrentUser();

            // Assert
            assertEquals(1, result.size());
            assertSame(response, result.get(0));
            verify(subscriptionRepository).findByUserId(1L);
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void update_Success_AppliesRequestAndCategory() {
            // Arrange
            Subscription owned = subscriptionOwnedBy(currentUser);
            Category newCategory = new Category("Music", "Music services");
            newCategory.setId(7L);
            request.setCategoryId(7L);

            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(owned));
            when(categoryRepository.findById(7L)).thenReturn(Optional.of(newCategory));
            when(subscriptionRepository.save(owned)).thenReturn(owned);
            when(subscriptionMapper.toResponse(owned)).thenReturn(response);

            // Act
            SubscriptionResponse result = subscriptionService.update(10L, request);

            // Assert
            assertSame(response, result);
            assertSame(newCategory, owned.getCategory());
            verify(subscriptionMapper).updateEntity(request, owned);
            verify(subscriptionRepository).save(owned);
        }

        @Test
        void update_SubscriptionNotFound_Throws() {
            // Arrange
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> subscriptionService.update(99L, request));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_Owned_Deletes() {
            // Arrange
            Subscription owned = subscriptionOwnedBy(currentUser);
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(owned));

            // Act
            subscriptionService.delete(10L);

            // Assert
            verify(subscriptionRepository).delete(owned);
        }

        @Test
        void delete_NotFound_Throws() {
            // Arrange
            when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> subscriptionService.delete(99L));
            verify(subscriptionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }
}