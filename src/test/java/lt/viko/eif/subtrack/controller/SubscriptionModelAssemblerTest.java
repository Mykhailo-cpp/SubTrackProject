package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.SubscriptionResponse;
import lt.viko.eif.subtrack.entity.BillingCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SubscriptionModelAssembler}.
 *
 * <p>Verifies that {@link SubscriptionModelAssembler#toModel(SubscriptionResponse)}
 * attaches the correct {@code self} and {@code subscriptions} HATEOAS links,
 * and that the original payload is preserved unchanged inside the model.</p>
 */
class SubscriptionModelAssemblerTest {

    private SubscriptionModelAssembler assembler;
    private SubscriptionResponse response;

    @BeforeEach
    void setUp() {
        assembler = new SubscriptionModelAssembler();

        response = new SubscriptionResponse(
                42L,
                "Netflix",
                "Standard plan",
                new BigDecimal("15.99"),
                "USD",
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 7, 1),
                true,
                false,
                1L,
                "Streaming"
        );
    }

    @Test
    void toModel_WrapsPayloadUnchanged() {
        EntityModel<SubscriptionResponse> model = assembler.toModel(response);

        assertNotNull(model.getContent(), "EntityModel content must not be null");
        assertEquals(response, model.getContent(), "EntityModel must wrap the original response");
    }

    @Test
    void toModel_HasSelfLink() {
        EntityModel<SubscriptionResponse> model = assembler.toModel(response);

        Link selfLink = model.getLink("self").orElse(null);
        assertNotNull(selfLink, "Model must contain a 'self' link");
        assertTrue(selfLink.getHref().endsWith("/api/subscriptions/42"),
                "Self link href must end with /api/subscriptions/42 but was: " + selfLink.getHref());
    }

    @Test
    void toModel_HasSubscriptionsCollectionLink() {
        EntityModel<SubscriptionResponse> model = assembler.toModel(response);

        Link collectionLink = model.getLink("subscriptions").orElse(null);
        assertNotNull(collectionLink, "Model must contain a 'subscriptions' link");
        assertTrue(collectionLink.getHref().endsWith("/api/subscriptions"),
                "Collection link href must end with /api/subscriptions but was: " + collectionLink.getHref());
    }

    @Test
    void toModel_SelfLinkReflectsSubscriptionId() {
        // Build a second response with a different id to confirm the link is dynamic
        SubscriptionResponse other = new SubscriptionResponse(
                99L,
                "Spotify",
                null,
                new BigDecimal("9.99"),
                "EUR",
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 8, 1),
                true,
                true,
                2L,
                "Music"
        );

        EntityModel<SubscriptionResponse> model = assembler.toModel(other);

        Link selfLink = model.getLink("self").orElseThrow();
        assertTrue(selfLink.getHref().endsWith("/api/subscriptions/99"),
                "Self link must use the response's own id (99) but was: " + selfLink.getHref());
    }
}