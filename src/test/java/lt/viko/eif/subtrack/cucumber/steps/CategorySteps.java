package lt.viko.eif.subtrack.cucumber.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.viko.eif.subtrack.cucumber.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Step definitions covering the {@code categories.feature} scenarios.
 *
 * <p>HATEOAS-wrapped responses (the controller returns
 * {@code EntityModel<CategoryResponse>} / {@code CollectionModel<...>}) are
 * parsed by drilling into {@code _embedded.categoryResponseList} for lists and
 * reading fields at the top level for single items (Spring HATEOAS serialises
 * {@code EntityModel} with the payload fields inlined alongside {@code _links}).</p>
 */
public class CategorySteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioContext ctx;

    // -------------------------------------------------------------------------
    // Given / preconditions
    // -------------------------------------------------------------------------

    /**
     * Idempotent helper: creates a category by name if it does not already
     * exist (409 from a duplicate is silently ignored). Stores the id in the
     * {@link ScenarioContext} for later use.
     */
    @Given("the category {string} already exists")
    public void theCategoryAlreadyExists(String name) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", name, "description", "Auto-created for test"));
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        int status = result.getResponse().getStatus();
        if (status == 201) {
            String json = result.getResponse().getContentAsString();
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            Number id = extractId(map);
            if (id != null) ctx.setLastCreatedId(id.longValue());
        }
        // 409 means it already existed — that's fine for a Given step
    }

    // -------------------------------------------------------------------------
    // When / actions
    // -------------------------------------------------------------------------

    @When("the user creates a category with name {string} and description {string}")
    public void theUserCreatesACategory(String name, String description) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", name, "description", description));
        ctx.setLastResult(
                mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)));

        // Capture the id when creation succeeds
        MvcResult result = ctx.getLastResult().andReturn();
        if (result.getResponse().getStatus() == 201) {
            Map<?, ?> map = objectMapper.readValue(
                    result.getResponse().getContentAsString(), Map.class);
            Number id = extractId(map);
            if (id != null) ctx.setLastCreatedId(id.longValue());
        }
    }

    @When("the user requests all categories")
    public void theUserRequestsAllCategories() throws Exception {
        ctx.setLastResult(
                mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + ctx.getAuthToken())));
    }

    // -------------------------------------------------------------------------
    // Then / assertions
    // -------------------------------------------------------------------------

    @And("the response body should contain the category name {string}")
    public void theResponseBodyShouldContainTheCategoryName(String expectedName) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(json, Map.class);
        // EntityModel inlines payload fields at the top level alongside _links
        assertThat(map.get("name"))
                .as("Expected category name '%s' in: %s", expectedName, json)
                .isEqualTo(expectedName);
    }

    @And("the category list should contain {string}")
    public void theCategoryListShouldContain(String expectedName) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> root = objectMapper.readValue(json, Map.class);

        // CollectionModel wraps items under "_embedded.categoryResponseList"
        List<?> items = extractEmbeddedList(root);

        boolean found = items.stream()
                .filter(item -> item instanceof Map)
                .map(item -> (Map<?, ?>) item)
                .anyMatch(m -> expectedName.equals(m.get("name")));
        assertThat(found)
                .as("Category list should contain '%s' but was: %s", expectedName, json)
                .isTrue();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the numeric {@code id} from an {@code EntityModel} response.
     * Spring HATEOAS inlines the payload fields at the top level, so {@code id}
     * is directly accessible on the root map.
     */
    private Number extractId(Map<?, ?> map) {
        Object id = map.get("id");
        return id instanceof Number ? (Number) id : null;
    }

    /**
     * Extracts the list of category items from a
     * {@code CollectionModel<EntityModel<CategoryResponse>>}.
     * Spring HATEOAS wraps items under {@code _embedded.<entityName>List}.
     */
    @SuppressWarnings("unchecked")
    private List<?> extractEmbeddedList(Map<?, ?> root) {
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
}