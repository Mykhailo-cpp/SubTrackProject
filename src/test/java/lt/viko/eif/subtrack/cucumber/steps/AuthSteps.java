package lt.viko.eif.subtrack.cucumber.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.viko.eif.subtrack.cucumber.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Step definitions covering the {@code auth.feature} scenarios.
 *
 * <p>All HTTP calls go through {@link MockMvc} — no real TCP port is opened —
 * while the full Spring Security filter chain (JWT filter, BCrypt encoder, etc.)
 * is exercised in every scenario.</p>
 */
public class AuthSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScenarioContext ctx;

    // -------------------------------------------------------------------------
    // Given steps — preconditions
    // -------------------------------------------------------------------------

    /**
     * Registers a user as a precondition; silently accepts 409 if the user
     * already exists so that Background steps are idempotent across scenarios.
     */
    @Given("a registered user with username {string}, email {string} and password {string}")
    public void aRegisteredUser(String username, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "email", email, "password", password));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        // 409 = already existed; that is fine for a Given step
    }

    // -------------------------------------------------------------------------
    // When steps — actions
    // -------------------------------------------------------------------------

    @When("a user registers with username {string}, email {string} and password {string}")
    public void aUserRegisters(String username, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "email", email, "password", password));
        ctx.setLastResult(
                mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)));
    }

    @When("the user logs in with username {string} and password {string}")
    public void theUserLogsIn(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password));
        ctx.setLastResult(
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)));
    }

    // -------------------------------------------------------------------------
    // Then / And steps — assertions
    // -------------------------------------------------------------------------

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) throws Exception {
        ctx.getLastResult().andExpect(status().is(expectedStatus));
    }

    @And("the response body should contain a JWT token")
    public void theResponseBodyShouldContainAJwtToken() throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(json, Map.class);
        String token = (String) map.get("token");
        assertThat(token).isNotBlank();
        // Store for authenticated follow-up steps in other feature files
        ctx.setAuthToken(token);
    }

    @And("the response body should contain the username {string}")
    public void theResponseBodyShouldContainTheUsername(String expectedUsername) throws Exception {
        MvcResult result = ctx.getLastResult().andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(json, Map.class);
        assertThat(map.get("username")).isEqualTo(expectedUsername);
    }

    // -------------------------------------------------------------------------
    // Shared authentication helper used from Background steps in other features
    // -------------------------------------------------------------------------

    @Given("the user is authenticated as {string} with password {string}")
    public void theUserIsAuthenticatedAs(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        Map<?, ?> map = objectMapper.readValue(json, Map.class);
        ctx.setAuthToken((String) map.get("token"));
    }
}