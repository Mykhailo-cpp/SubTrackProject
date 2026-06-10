package com.subtrack.service;

import com.subtrack.entity.BillingCycle;
import com.subtrack.entity.Category;
import com.subtrack.entity.Subscription;
import com.subtrack.entity.User;
import com.subtrack.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RenewalReminderServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RenewalReminderServiceImpl renewalReminderService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "test@example.com", "hashedPassword");

        Category category = new Category();
        category.setName("Entertainment");

        subscription = new Subscription(
                "Spotify",
                "Music streaming",
                new BigDecimal("9.99"),
                "EUR",
                BillingCycle.MONTHLY,
                LocalDate.now().plusDays(3),
                true
        );
        subscription.setUser(user);
        subscription.setCategory(category);
    }

    @Nested
    class SendUpcomingRenewalRemindersTests {

        @Test
        void sendUpcomingRenewalReminders_OneUpcomingSubscription_SendsOneEmail() {
            // Arrange
            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of(subscription));

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService, times(1)).sendEmail(
                    eq("test@example.com"),
                    contains("Spotify"),
                    anyString()
            );
        }

        @Test
        void sendUpcomingRenewalReminders_NoUpcomingSubscriptions_SendsNoEmails() {
            // Arrange
            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of());

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        void sendUpcomingRenewalReminders_InactiveSubscription_SkipsEmail() {
            // Arrange
            subscription.setActive(false);
            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of(subscription));

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        void sendUpcomingRenewalReminders_UserWithNoEmail_SkipsEmail() {
            // Arrange
            user.setEmail("");
            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of(subscription));

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        void sendUpcomingRenewalReminders_EmailSubjectContainsSubscriptionName() {
            // Arrange
            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of(subscription));

            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService).sendEmail(anyString(), subjectCaptor.capture(), anyString());
            assertTrue(subjectCaptor.getValue().contains("Spotify"));
        }

        @Test
        void sendUpcomingRenewalReminders_EmailBodyContainsSubscriptionDetails() {
            // Arrange
            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of(subscription));

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService).sendEmail(anyString(), anyString(), bodyCaptor.capture());
            String body = bodyCaptor.getValue();
            assertTrue(body.contains("Spotify"));
            assertTrue(body.contains("9.99"));
            assertTrue(body.contains("EUR"));
            assertTrue(body.contains("testuser"));
        }

        @Test
        void sendUpcomingRenewalReminders_MultipleSubscriptions_SendsEmailForEach() {
            // Arrange
            Subscription netflix = new Subscription(
                    "Netflix",
                    null,
                    new BigDecimal("15.99"),
                    "EUR",
                    BillingCycle.MONTHLY,
                    LocalDate.now().plusDays(5),
                    true
            );
            netflix.setUser(user);

            when(subscriptionRepository.findByNextRenewalDateBefore(any(LocalDate.class)))
                    .thenReturn(List.of(subscription, netflix));

            // Act
            renewalReminderService.sendUpcomingRenewalReminders();

            // Assert
            verify(emailService, times(2)).sendEmail(anyString(), anyString(), anyString());
        }
    }
}
