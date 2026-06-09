package com.subtrack.controller;

import com.subtrack.dto.SpendingSummaryResponse;
import com.subtrack.dto.SubscriptionRequest;
import com.subtrack.dto.SubscriptionResponse;
import com.subtrack.service.SpendingService;
import com.subtrack.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller exposing CRUD endpoints for subscriptions under
 * {@code /api/subscriptions}.
 *
 * Every endpoint operates only on the authenticated user's own
 * subscriptions; requesting a subscription that does not exist or belongs to
 * another user yields HTTP 404. Responses are wrapped in HATEOAS models
 * carrying {@code self} (and collection) links, satisfying Richardson
 * Maturity Model Level 3. Ownership scoping and persistence are delegated to
 * {@link SubscriptionService}; link construction is delegated to
 * {@link SubscriptionModelAssembler}
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    /** Service encapsulating subscription business logic. */
    private final SubscriptionService subscriptionService;

    /** Service for computing spending summaries for the current user. */
    private final SpendingService spendingService;

    /** Assembler that attaches HATEOAS links to responses. */
    private final SubscriptionModelAssembler assembler;

    /**
     * Creates the controller with its required collaborators.
     *
     * @param subscriptionService the subscription service
     * @param assembler           the HATEOAS model assembler
     */
    public SubscriptionController(SubscriptionService subscriptionService,
                                  SpendingService spendingService,
                                  SubscriptionModelAssembler assembler) {
        this.subscriptionService = subscriptionService;
        this.spendingService = spendingService;
        this.assembler = assembler;
    }

    /**
     * Returns all subscriptions for the authenticated user.
     *
     * @return HTTP 200 with a collection model of link-enriched subscriptions
     */
    @GetMapping
    public CollectionModel<EntityModel<SubscriptionResponse>> getAllSubscriptions() {
        List<EntityModel<SubscriptionResponse>> models =
                subscriptionService.getAllForCurrentUser().stream()
                        .map(assembler::toModel)
                        .toList();
        return CollectionModel.of(models,
                linkTo(methodOn(SubscriptionController.class).getAllSubscriptions()).withSelfRel());
    }

    /**
     * Returns a single subscription belonging to the authenticated user.
     *
     * @param id the subscription id
     * @return HTTP 200 with the link-enriched subscription;
     *         HTTP 404 if it does not exist or belongs to another user
     */
    @GetMapping("/{id}")
    public EntityModel<SubscriptionResponse> getSubscription(@PathVariable Long id) {
        return assembler.toModel(subscriptionService.getByIdForCurrentUser(id));
    }

    /**
     * Creates a subscription linked to the authenticated user.
     *
     * @param request the validated subscription details
     * @return HTTP 201 with the created subscription, a {@code Location} header,
     *         and HATEOAS links; HTTP 404 if the referenced category does not exist
     */
    @PostMapping
    public ResponseEntity<EntityModel<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        EntityModel<SubscriptionResponse> model =
                assembler.toModel(subscriptionService.create(request));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    /**
     * Updates a subscription belonging to the authenticated user.
     *
     * @param id      the id of the subscription to update
     * @param request the validated new subscription details
     * @return HTTP 200 with the updated, link-enriched subscription;
     *         HTTP 404 if it does not exist, belongs to another user, or the
     *         referenced category does not exist
     */
    @PutMapping("/{id}")
    public EntityModel<SubscriptionResponse> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request) {
        return assembler.toModel(subscriptionService.update(id, request));
    }

    /**
     * Deletes a subscription belonging to the authenticated user.
     *
     * @param id the id of the subscription to delete
     * @return HTTP 204 (No Content); HTTP 404 if it does not exist or belongs to another user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a spending summary for the authenticated user, grouped by category.
     *
     * <p>Only active subscriptions are included. All prices are normalised to
     * monthly and yearly equivalents in their original currencies.</p>
     *
     * @return HTTP 200 with the spending summary
     */
    @GetMapping("/summary")
    @Operation(summary = "Get monthly and yearly spending summary grouped by category")
    public ResponseEntity<SpendingSummaryResponse> getSpendingSummary() {
        return ResponseEntity.ok(spendingService.getSummaryForCurrentUser());
    }
}