package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.CurrencyConversionResponse;
import lt.viko.eif.subtrack.dto.SpendingSummaryResponse;
import lt.viko.eif.subtrack.dto.SubscriptionRequest;
import lt.viko.eif.subtrack.dto.SubscriptionResponse;
import lt.viko.eif.subtrack.service.CurrencyService;
import lt.viko.eif.subtrack.service.RenewalReminderService;
import lt.viko.eif.subtrack.service.SpendingService;
import lt.viko.eif.subtrack.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller exposing CRUD endpoints for subscriptions under
 * {@code /api/subscriptions}.
 *
 * <p>Every endpoint operates only on the authenticated user's own
 * subscriptions; requesting a subscription that does not exist or belongs to
 * another user yields HTTP 404. Responses are wrapped in HATEOAS models
 * carrying {@code self} (and collection) links, satisfying Richardson
 * Maturity Model Level 3. Ownership scoping and persistence are delegated to
 * {@link SubscriptionService}; link construction is delegated to
 * {@link SubscriptionModelAssembler}.</p>
 */
@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscriptions", description = "Manage the authenticated user's paid subscriptions")
public class SubscriptionController {

    /** Service encapsulating subscription business logic. */
    private final SubscriptionService subscriptionService;

    /** Service for computing spending summaries for the current user. */
    private final SpendingService spendingService;

    /** Assembler that attaches HATEOAS links to responses. */
    private final SubscriptionModelAssembler assembler;

    private final CurrencyService currencyService;

    private final RenewalReminderService renewalReminderService;

    /**
     * Creates the controller with its required collaborators.
     *
     * @param subscriptionService   the subscription service
     * @param spendingService       the spending summary service
     * @param assembler             the HATEOAS model assembler
     * @param currencyService       the currency conversion service
     * @param renewalReminderService the renewal reminder service
     */
    public SubscriptionController(SubscriptionService subscriptionService,
                                  SpendingService spendingService,
                                  SubscriptionModelAssembler assembler,
                                  CurrencyService currencyService,
                                  RenewalReminderService renewalReminderService) {
        this.subscriptionService = subscriptionService;
        this.spendingService = spendingService;
        this.assembler = assembler;
        this.currencyService = currencyService;
        this.renewalReminderService = renewalReminderService;
    }

    /**
     * Returns all subscriptions for the authenticated user.
     *
     * @return HTTP 200 with a collection model of link-enriched subscriptions
     */
    @GetMapping
    @Operation(
            summary = "Get all subscriptions",
            description = "Returns every subscription that belongs to the currently authenticated user. " +
                    "Each item is enriched with a `self` link and a `subscriptions` collection link (HATEOAS)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Collection of subscriptions returned (may be empty)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "_embedded": {
                                        "subscriptionResponseList": [
                                          {
                                            "id": 1,
                                            "name": "Netflix",
                                            "price": 15.99,
                                            "currency": "USD",
                                            "billingCycle": "MONTHLY",
                                            "nextRenewalDate": "2025-07-01",
                                            "active": true,
                                            "categoryId": 1,
                                            "_links": {
                                              "self":          { "href": "http://localhost:8080/api/subscriptions/1" },
                                              "subscriptions": { "href": "http://localhost:8080/api/subscriptions" }
                                            }
                                          }
                                        ]
                                      },
                                      "_links": {
                                        "self": { "href": "http://localhost:8080/api/subscriptions" }
                                      }
                                    }""")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }""")))
    })
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
    @Operation(
            summary = "Get a subscription by ID",
            description = "Returns the subscription with the given `id`, provided it belongs to the " +
                    "authenticated user. A subscription owned by another user is indistinguishable " +
                    "from a non-existent one — both yield HTTP 404."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subscription found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubscriptionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "name": "Netflix",
                                      "price": 15.99,
                                      "currency": "USD",
                                      "billingCycle": "MONTHLY",
                                      "nextRenewalDate": "2025-07-01",
                                      "active": true,
                                      "categoryId": 1,
                                      "_links": {
                                        "self":          { "href": "http://localhost:8080/api/subscriptions/1" },
                                        "subscriptions": { "href": "http://localhost:8080/api/subscriptions" }
                                      }
                                    }""")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }"""))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subscription not found or belongs to another user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Subscription with id 99 not found" }""")
                    )
            )
    })
    public EntityModel<SubscriptionResponse> getSubscription(
            @Parameter(description = "ID of the subscription to retrieve", required = true, example = "1")
            @PathVariable Long id) {
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
    @Operation(
            summary = "Create a new subscription",
            description = "Persists a new subscription and associates it with the authenticated user. " +
                    "Returns HTTP 201 with a `Location` header pointing to the new resource."
    )
    @RequestBody(
            description = "Subscription details. `categoryId` must reference an existing category.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SubscriptionRequest.class),
                    examples = @ExampleObject(
                            name = "Monthly Netflix",
                            value = """
                                    {
                                      "name": "Netflix",
                                      "price": 15.99,
                                      "currency": "USD",
                                      "billingCycle": "MONTHLY",
                                      "nextRenewalDate": "2025-07-01",
                                      "active": true,
                                      "categoryId": 1
                                    }"""
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Subscription created — `Location` header points to the new resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubscriptionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 5,
                                      "name": "Netflix",
                                      "price": 15.99,
                                      "currency": "USD",
                                      "billingCycle": "MONTHLY",
                                      "nextRenewalDate": "2025-07-01",
                                      "active": true,
                                      "categoryId": 1,
                                      "_links": {
                                        "self":          { "href": "http://localhost:8080/api/subscriptions/5" },
                                        "subscriptions": { "href": "http://localhost:8080/api/subscriptions" }
                                      }
                                    }""")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "price: must be greater than 0" }"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }"""))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced category does not exist",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Category with id 99 not found" }""")
                    )
            )
    })
    public ResponseEntity<EntityModel<SubscriptionResponse>> createSubscription(
            @Valid @org.springframework.web.bind.annotation.RequestBody SubscriptionRequest request) {
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
    @Operation(
            summary = "Update a subscription",
            description = "Fully replaces the subscription identified by `id` with the supplied data. " +
                    "The subscription must belong to the authenticated user."
    )
    @RequestBody(
            description = "Updated subscription data.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SubscriptionRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "name": "Netflix Premium",
                              "price": 22.99,
                              "currency": "USD",
                              "billingCycle": "MONTHLY",
                              "nextRenewalDate": "2025-08-01",
                              "active": true,
                              "categoryId": 1
                            }""")
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Subscription updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubscriptionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "name": "Netflix Premium",
                                      "price": 22.99,
                                      "currency": "USD",
                                      "billingCycle": "MONTHLY",
                                      "nextRenewalDate": "2025-08-01",
                                      "active": true,
                                      "categoryId": 1,
                                      "_links": {
                                        "self":          { "href": "http://localhost:8080/api/subscriptions/1" },
                                        "subscriptions": { "href": "http://localhost:8080/api/subscriptions" }
                                      }
                                    }""")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "name: must not be blank" }"""))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }"""))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subscription not found, belongs to another user, or category not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Subscription with id 99 not found" }""")
                    )
            )
    })
    public EntityModel<SubscriptionResponse> updateSubscription(
            @Parameter(description = "ID of the subscription to update", required = true, example = "1")
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody SubscriptionRequest request) {
        return assembler.toModel(subscriptionService.update(id, request));
    }

    /**
     * Deletes a subscription belonging to the authenticated user.
     *
     * @param id the id of the subscription to delete
     * @return HTTP 204 (No Content); HTTP 404 if it does not exist or belongs to another user
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a subscription",
            description = "Permanently removes the subscription identified by `id`. " +
                    "The subscription must belong to the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subscription deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }"""))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subscription not found or belongs to another user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Subscription with id 99 not found" }""")
                    )
            )
    })
    public ResponseEntity<Void> deleteSubscription(
            @Parameter(description = "ID of the subscription to delete", required = true, example = "1")
            @PathVariable Long id) {
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
    @Operation(
            summary = "Get spending summary",
            description = "Returns monthly and yearly spending totals grouped by category. " +
                    "Only **active** subscriptions are included. " +
                    "Amounts are kept in each subscription's original currency — no cross-currency " +
                    "aggregation is performed."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Spending summary returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SpendingSummaryResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "categories": [
                                        {
                                          "categoryName": "Streaming",
                                          "monthlyTotal": 31.98,
                                          "yearlyTotal": 383.76,
                                          "currency": "USD"
                                        },
                                        {
                                          "categoryName": "Software",
                                          "monthlyTotal": 9.99,
                                          "yearlyTotal": 119.88,
                                          "currency": "EUR"
                                        }
                                      ]
                                    }""")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }""")))
    })
    public ResponseEntity<SpendingSummaryResponse> getSpendingSummary() {
        return ResponseEntity.ok(spendingService.getSummaryForCurrentUser());
    }

    /**
     * Converts a subscription's price to the requested currency.
     *
     * <p>Ownership is enforced — requesting a subscription that does not exist
     * or belongs to another user yields HTTP 404. The original price and currency
     * are preserved in the response alongside the converted amount and the
     * exchange rate applied.</p>
     *
     * @param id             the id of the subscription to convert
     * @param targetCurrency the ISO 4217 target currency code (e.g. {@code "USD"})
     * @return HTTP 200 with the conversion result;
     *         HTTP 404 if the subscription does not exist or belongs to another user;
     *         HTTP 400 if the target currency code is unsupported
     */
    @GetMapping("/{id}/convert")
    @Operation(
            summary = "Convert subscription price to another currency",
            description = "Looks up the live exchange rate and converts the subscription's price " +
                    "to the requested `targetCurrency`. The original price, original currency, " +
                    "converted amount, and applied rate are all included in the response."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Conversion successful",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CurrencyConversionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "subscriptionId": 1,
                                      "originalAmount": 15.99,
                                      "originalCurrency": "USD",
                                      "convertedAmount": 14.83,
                                      "targetCurrency": "EUR",
                                      "exchangeRate": 0.9275
                                    }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Unsupported or invalid target currency code",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unsupported currency: XYZ" }""")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }"""))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Subscription not found or belongs to another user",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Subscription with id 99 not found" }""")
                    )
            )
    })
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Parameter(description = "ID of the subscription to convert", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ISO 4217 target currency code", required = true, example = "EUR")
            @RequestParam String targetCurrency) {
        return ResponseEntity.ok(currencyService.convert(id, targetCurrency));
    }

    /**
     * Manually triggers the renewal-reminder job for testing purposes.
     *
     * <p><strong>Note:</strong> This endpoint is intended for development and
     * should be removed or secured before going to production.</p>
     *
     * @return HTTP 200 confirmation message
     */
    @GetMapping("/test-reminder")
    @Operation(
            summary = "Trigger renewal reminder (dev only)",
            description = "Manually fires the scheduled renewal-reminder job. " +
                    "**Remove or secure this endpoint before deploying to production.**"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reminder job triggered successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            examples = @ExampleObject(value = "Reminder job triggered")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Unauthorized" }""")))
    })
    public ResponseEntity<String> testReminder() {
        renewalReminderService.sendUpcomingRenewalReminders();
        return ResponseEntity.ok("Reminder job triggered");
    }
}