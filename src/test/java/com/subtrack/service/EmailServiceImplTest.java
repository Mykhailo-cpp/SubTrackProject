package com.subtrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@subtrack.com");
    }

    @Nested
    class SendEmailTests {

        @Test
        void sendEmail_Success_SendsMessageWithCorrectFields() {
            // Arrange
            String to = "user@example.com";
            String subject = "SubTrack — Renewal reminder: Spotify";
            String body = "Hi testuser, your Spotify subscription renews on 2026-06-17.";

            ArgumentCaptor<SimpleMailMessage> captor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);

            // Act
            emailService.sendEmail(to, subject, body);

            // Assert
            verify(mailSender, times(1)).send(captor.capture());

            SimpleMailMessage sent = captor.getValue();
            assertEquals("noreply@subtrack.com", sent.getFrom());
            assertArrayEquals(new String[]{to}, sent.getTo());
            assertEquals(subject, sent.getSubject());
            assertEquals(body, sent.getText());
        }

        @Test
        void sendEmail_MailSenderThrows_DoesNotPropagateException() {
            // Arrange
            doThrow(new MailSendException("SMTP unavailable"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            // Act & Assert — must not throw
            assertDoesNotThrow(() ->
                    emailService.sendEmail("user@example.com", "Subject", "Body"));

            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        void sendEmail_MailSenderThrows_StillAttemptsDelivery() {
            // Arrange
            doThrow(new MailSendException("Connection refused"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            // Act
            emailService.sendEmail("user@example.com", "Subject", "Body");

            // Assert — send was attempted exactly once despite the failure
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
    }
}
