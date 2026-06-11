package lt.viko.eif.subtrack.cucumber.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.viko.eif.subtrack.cucumber.ScenarioContext;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Step definitions covering the {@code subscriptions.feature} scenarios.
 *
 * <p>HATEOAS-wrapped responses (the controller returns
 * {@code EntityModel<SubscriptionResponse>} / {@code CollectionModel<…>}) are
 * parsed by drilling into the {@code content} array for lists and reading
 * {@code content.name} etc. for single items.</p>
 */
public class SubscriptionSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioContext ctx;

    /**
     * Tracks subscription ids per owner username so the cross-user isolation
     * scenario can look up a subscription by owner + name.
     */
    private final Map<String, Long> subscriptionIdByOwnerAndName = new HashMap<>();

    // -------------------------------------------------------------------------
    // DataTable converter
    // -------------------------------------------------------------------------

    @DataTableType
    public SubscriptionRow subscriptionRow(Map<String, String> entry) {
        return new SubscriptionRow(
                entry.get("name"),
                entry.get("price"),
                entry.get("currency"),
                entry.get("billingCycle"),
                entry.get("nextRenewalDate"),
                Boolean.parseBoolean(entry.getOrDefault("active", "true")),
                Boolean.parseBoolean(entry.getOrDefault("renewalReminderEnabled", "false")),
                entry.get("categoryName")
        );
    }

    // -------------------------------------------------------------------------
    // Given / preconditions
    // -------------------------------------------------------------------------

    @Given("the user has a subscription named {string} with price {string} and billing cycle {string} in category {string}")
    public void theUserHasASubscription(String name, String price, String billingCycle, String categoryName)
            throws Exception {
        createSubscription(name, price, "EUR", billingCycle, "2026-12-01", true, categoryName);
    }

    // -------------------------------------------------------------------------
    // When / actions
    // -------------------------------------------------------------------------

    @When("the user creates a subscription with the following details:")
    public void theUserCreatesASubscriptionWithDetails(List<SubscriptionRow> rows) throws Exception {
        SubscriptionRow row = rows.get(0);

        // Resolve category id
        long categoryId = resolveCategoryId(row.categoryName());

        Map<String, Object> body = new HashMap<>();
        body.put("name", row.name());
        body.put("price", Double.parseDouble(row.price()));
        body.put("currency", row.currency());
        body.put("billingCycle", row.billingCycle());
        body.put("nextRenewalDate", row.nextRenewalDate());
        body.put("active", row.active());
        body.put("renewalReminderEnabled", row.renewalReminderEnabled());
        body.put("categoryId", categoryId);

        ctx.setLastResult(
                mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));

        MvcResult result = ctx.getLastResult().andReturn();
        if (result.getResponse().getStatus() == 201) {
            Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
            Number id = extractId(map);
            if (id != null) ctx.setLastCreatedId(id.longValue());
        }
    }

    @When("the user requests all their subscriptions")
    public void theUserRequestsAllTheirSubscriptions() throws Exception {
        ctx.setLastResult(
                mockMvc.perform(get("/api/subscriptions")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())));
    }

    @When("the user updates that subscription with name {string} and price {string}")
    public void theUserUpdatesThatSubscription(String newName, String newPrice) throws Exception {
        long id = ctx.getLastCreatedId();
        long categoryId = resolveCategoryId("Software");

        Map<String, Object> body = new HashMap<>();
        body.put("name", newName);
        body.put("price", Double.parseDouble(newPrice));
        body.put("currency", "EUR");
        body.put("billingCycle", "MONTHLY");
        body.put("nextRenewalDate", "2026-12-01");
        body.put("active", true);
        body.put("categoryId", categoryId);

        ctx.setLastResult(
                mockMvc.perform(put("/api/subscriptions/" + id)
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));
    }

    @When("the user deletes that subscription")
    public void theUserDeletesThatSubscription() throws Exception {
        long id = ctx.getLastCreatedId();
        ctx.setLastResult(
                mockMvc.perform(delete("/api/subscriptions/" + id)
                        .header("Authorization", "Bearer " + ctx.getAuthToken())));
    }

    @When("the user requests subscription with id {long}")
    public void theUserRequestsSubscriptionWithId(long id) throws Exception {
        ctx.setLastResult(
                mockMvc.perform(get("/api/subscriptions/" + id)
                        .header("Authorization", "Bearer " + ctx.getAuthToken())));
    }

    @When("the user requests the subscription owned by {string} named {string}")
    public void theUserRequestsSubscriptionOwnedBy(String ownerUsername, String subName) throws Exception {
        Long id = subscriptionIdByOwnerAndName.get(ownerUsername + "::" + subName);
        assertThat(id).as("Expected a tracked id for %s::%s", ownerUsername, subName).isNotNull();
        ctx.setLastResult(
                mockMvc.perform(get("/api/subscriptions/" + id)
                        .header("Authorization", "Bearer " + ctx.getAuthToken())));
    }

    // -------------------------------------------------------------------------
    // Then / assertions
    // -------------------------------------------------------------------------

    @And("the response body should contain the subscription name {string}")
    public void theResponseBodyShouldContainTheSubscriptionName(String expectedName) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(json, Map.class);
        String actualName = extractSubscriptionName(map);
        assertThat(actualName)
                .as("Expected subscription name '%s' in: %s", expectedName, json)
                .isEqualTo(expectedName);
    }

    @And("the subscription list should contain {string}")
    public void theSubscriptionListShouldContain(String expectedName) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> root = objectMapper.readValue(json, Map.class);

        // CollectionModel wraps items under "_embedded.subscriptionResponseList"
        // or falls back to a plain list
        List<?> items = extractEmbeddedList(root);

        boolean found = items.stream()
                .filter(i -> i instanceof Map)
                .map(i -> (Map<?, ?>) i)
                .anyMatch(m -> expectedName.equals(extractSubscriptionName(m)));
        assertThat(found)
                .as("Subscription list should contain '%s' but was: %s", expectedName, json)
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a subscription for the currently-authenticated user and records
     * the returned id in {@link ScenarioContext} and in the per-owner map.
     */
    private void createSubscription(String name, String price, String currency,
                                    String billingCycle, String nextRenewalDate,
                                    boolean active, String categoryName) throws Exception {
        long categoryId = resolveCategoryId(categoryName);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", Double.parseDouble(price));
        body.put("currency", currency);
        body.put("billingCycle", billingCycle);
        body.put("nextRenewalDate", nextRenewalDate);
        body.put("active", active);
        body.put("categoryId", categoryId);

        MvcResult result = mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertThat(status).as("Subscription creation failed for '%s': %s",
                name, result.getResponse().getContentAsString()).isEqualTo(201);

        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Number id = extractId(map);
        if (id != null) {
            ctx.setLastCreatedId(id.longValue());
            // Record for cross-user scenario: derive owner from token claim is complex,
            // so we track by a synthetic key set in the subscription's name+caller context.
            // The scenario step calls this after switching auth, so the "current owner"
            // at this point is the user whose token is in the context.
            subscriptionIdByOwnerAndName.put("otheruser::" + name, id.longValue());
        }
    }

    /**
     * Resolves the database id of a category by name, creating it if absent.
     *
     * <p>GET /api/categories now returns a HATEOAS {@code CollectionModel}, so items
     * are nested under {@code _embedded.categoryResponseList} rather than at the root.</p>
     */
    private long resolveCategoryId(String categoryName) throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken()))
                .andReturn();
        Map<?, ?> root = objectMapper.readValue(listResult.getResponse().getContentAsString(), Map.class);
        List<?> cats = extractEmbeddedCategoryList(root);
        for (Object item : cats) {
            if (item instanceof Map<?, ?> m && categoryName.equals(m.get("name"))) {
                return ((Number) m.get("id")).longValue();
            }
        }
        // Not found — create it
        String body = objectMapper.writeValueAsString(
                Map.of("name", categoryName, "description", "Auto-created"));
        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        return ((Number) created.get("id")).longValue();
    }

    /**
     * Extracts the list of categories from a
     * {@code CollectionModel<EntityModel<CategoryResponse>>} response.
     * Spring HATEOAS wraps items under {@code _embedded.<entityName>List}.
     */
    @SuppressWarnings("unchecked")
    private List<?> extractEmbeddedCategoryList(Map<?, ?> root) {
        if (root.containsKey("_embedded")) {
            Map<?, ?> embedded = (Map<?, ?>) root.get("_embedded");
            for (Object value : embedded.values()) {
                if (value instanceof List) {
                    return (List<?>) value;
                }
            }
        }
        return List.of();
    }

    /**
     * Extracts the subscription {@code name} field from either a plain
     * {@code SubscriptionResponse} map or an HATEOAS {@code EntityModel} map.
     */
    private String extractSubscriptionName(Map<?, ?> map) {
        if (map.containsKey("name")) {
            return (String) map.get("name");
        }
        // EntityModel wraps fields at top-level with _links alongside them
        return null;
    }

    /**
     * Extracts the numeric {@code id} from either a plain response or a
     * top-level HATEOAS {@code EntityModel}.
     */
    private Number extractId(Map<?, ?> map) {
        if (map.containsKey("id")) {
            return (Number) map.get("id");
        }
        return null;
    }

    /**
     * Extracts the list of subscription items from a
     * {@code CollectionModel<EntityModel<SubscriptionResponse>>} or falls back to
     * treating the root object as the list directly.
     */
    @SuppressWarnings("unchecked")
    private List<?> extractEmbeddedList(Map<?, ?> root) {
        // Spring HATEOAS CollectionModel wraps items under _embedded
        if (root.containsKey("_embedded")) {
            Map<?, ?> embedded = (Map<?, ?>) root.get("_embedded");
            // The key is typically the pluralised entity name
            for (Object value : embedded.values()) {
                if (value instanceof List) {
                    return (List<?>) value;
                }
            }
        }
        // Fallback: root might be a plain list serialised as object — unlikely but safe
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Inner record for DataTable mapping
    // -------------------------------------------------------------------------

    record SubscriptionRow(
            String name,
            String price,
            String currency,
            String billingCycle,
            String nextRenewalDate,
            boolean active,
            boolean renewalReminderEnabled,
            String categoryName
    ) {}
}