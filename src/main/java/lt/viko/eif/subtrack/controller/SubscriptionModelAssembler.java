package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.SubscriptionResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Converts a {@link SubscriptionResponse} into an {@link EntityModel} enriched
 * with HATEOAS links, satisfying Richardson Maturity Model Level 3.
 *
 * Each model carries a {@code self} link to its own resource and a
 * {@code subscriptions} link back to the collection. Links are derived from
 * the {@link SubscriptionController} mappings via {@code linkTo(methodOn(...))}
 * so that they stay correct if the URL structure changes
 */
@Component
public class SubscriptionModelAssembler
        implements RepresentationModelAssembler<SubscriptionResponse, EntityModel<SubscriptionResponse>> {

    /**
     * Wraps the given response in an {@link EntityModel} with self and
     * collection links.
     *
     * @param subscription the response DTO to wrap
     * @return the link-enriched model
     */
    @Override
    public EntityModel<SubscriptionResponse> toModel(SubscriptionResponse subscription) {
        return EntityModel.of(subscription,
                linkTo(methodOn(SubscriptionController.class)
                        .getSubscription(subscription.id())).withSelfRel(),
                linkTo(methodOn(SubscriptionController.class)
                        .getAllSubscriptions()).withRel("subscriptions"));
    }
}