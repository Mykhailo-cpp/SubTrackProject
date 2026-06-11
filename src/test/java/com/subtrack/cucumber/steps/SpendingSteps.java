package com.subtrack.cucumber.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subtrack.cucumber.ScenarioContext;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Step definitions covering the {@code spending.feature} scenarios.
 *
 * <p>The summary endpoint returns a {@link com.subtrack.dto.SpendingSummaryResponse}
 * whose structure is:</p>
 * <pre>
 * {
 *   "categories": [
 *     {
 *       "categoryName": "Entertainment",
 *       "subscriptions": [
 *         {
 *           "name": "Netflix",
 *           "originalPrice": 15.99,
 *           "currency": "EUR",
 *           "monthlyPrice": 15.99,
 *           "yearlyPrice": 191.88
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 */
public class SpendingSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioContext ctx;

    // -------------------------------------------------------------------------
    // Given / preconditions
    // -------------------------------------------------------------------------

    @Given("the user has an active subscription named {string} with price {string}, billing cycle {string}, currency {string} in category {string}")
    public void theUserHasAnActiveSubscription(String name, String price, String billingCycle,
                                               String currency, String categoryName) throws Exception {
        createSubscription(name, price, currency, billingCycle, true, categoryName);
    }

    @Given("the user has an inactive subscription named {string} with price {string}, billing cycle {string}, currency {string} in category {string}")
    public void theUserHasAnInactiveSubscription(String name, String price, String billingCycle,
                                                 String currency, String categoryName) throws Exception {
        createSubscription(name, price, currency, billingCycle, false, categoryName);
    }

    // -------------------------------------------------------------------------
    // When / actions
    // -------------------------------------------------------------------------

    @When("the user requests the spending summary")
    public void theUserRequestsTheSpendingSummary() throws Exception {
        ctx.setLastResult(
                mockMvc.perform(get("/api/subscriptions/summary")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())));
    }

    // -------------------------------------------------------------------------
    // Then / assertions
    // -------------------------------------------------------------------------

    @And("the summary should contain a subscription named {string} with monthly price {string}")
    public void theSummaryShouldContainSubscriptionWithMonthlyPrice(String expectedName,
                                                                    String expectedMonthly) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> root = objectMapper.readValue(json, Map.class);

        List<?> categories = (List<?>) root.get("categories");
        assertThat(categories)
                .as("Expected non-empty categories in spending summary but was: %s", json)
                .isNotNull()
                .isNotEmpty();

        boolean found = false;
        for (Object catObj : categories) {
            Map<?, ?> cat = (Map<?, ?>) catObj;
            List<?> subscriptions = (List<?>) cat.get("subscriptions");
            if (subscriptions == null) continue;
            for (Object subObj : subscriptions) {
                Map<?, ?> sub = (Map<?, ?>) subObj;
                if (expectedName.equals(sub.get("name"))) {
                    Number actualMonthly = (Number) sub.get("monthlyPrice");
                    assertThat(actualMonthly).isNotNull();
                    assertThat(actualMonthly.doubleValue())
                            .as("Monthly price for '%s'", expectedName)
                            .isEqualTo(Double.parseDouble(expectedMonthly));
                    found = true;
                }
            }
        }
        assertThat(found)
                .as("Subscription '%s' not found in summary: %s", expectedName, json)
                .isTrue();
    }

    @And("the summary should not contain a subscription named {string}")
    public void theSummaryShouldNotContainSubscriptionNamed(String unexpectedName) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> root = objectMapper.readValue(json, Map.class);

        List<?> categories = (List<?>) root.get("categories");
        if (categories == null || categories.isEmpty()) {
            return; // No categories at all — definitely not present
        }

        boolean found = false;
        for (Object catObj : categories) {
            Map<?, ?> cat = (Map<?, ?>) catObj;
            List<?> subscriptions = (List<?>) cat.get("subscriptions");
            if (subscriptions == null) continue;
            for (Object subObj : subscriptions) {
                Map<?, ?> sub = (Map<?, ?>) subObj;
                if (unexpectedName.equals(sub.get("name"))) {
                    found = true;
                }
            }
        }
        assertThat(found)
                .as("Inactive subscription '%s' should NOT appear in summary but it did: %s",
                        unexpectedName, json)
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void createSubscription(String name, String price, String currency,
                                    String billingCycle, boolean active,
                                    String categoryName) throws Exception {
        long categoryId = resolveCategoryId(categoryName);

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", Double.parseDouble(price));
        body.put("currency", currency);
        body.put("billingCycle", billingCycle);
        body.put("nextRenewalDate", "2026-12-01");
        body.put("active", active);
        body.put("categoryId", categoryId);

        MvcResult result = mockMvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        assertThat(result.getResponse().getStatus())
                .as("Failed to create subscription '%s': %s",
                        name, result.getResponse().getContentAsString())
                .isEqualTo(201);
    }

    private long resolveCategoryId(String categoryName) throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken()))
                .andReturn();
        List<?> cats = objectMapper.readValue(listResult.getResponse().getContentAsString(), List.class);
        for (Object item : cats) {
            if (item instanceof Map<?, ?> m && categoryName.equals(m.get("name"))) {
                return ((Number) m.get("id")).longValue();
            }
        }
        // Not found — create it
        String body = objectMapper.writeValueAsString(
                Map.of("name", categoryName, "description", "Auto-created for spending test"));
        MvcResult createResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        Map<?, ?> created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        return ((Number) created.get("id")).longValue();
    }
}