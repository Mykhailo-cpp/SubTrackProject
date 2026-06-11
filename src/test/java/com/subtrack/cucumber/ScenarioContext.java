package com.subtrack.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Per-scenario state bag shared across all step-definition classes.
 *
 * <p>Cucumber-Spring creates one instance of this component per scenario
 * (thanks to {@link ScenarioScope}) and injects it wherever it is needed,
 * allowing step definitions to pass data between Given/When/Then steps without
 * static fields or thread-locals.</p>
 */
@Component
@ScenarioScope
public class ScenarioContext {

    /** JWT Bearer token obtained after a successful login/register. */
    private String authToken;

    /** The most recent MockMvc result, available for Then-step assertions. */
    private ResultActions lastResult;

    /** Id of the most recently created resource (category, subscription, …). */
    private Long lastCreatedId;

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public ResultActions getLastResult() {
        return lastResult;
    }

    public void setLastResult(ResultActions lastResult) {
        this.lastResult = lastResult;
    }

    public Long getLastCreatedId() {
        return lastCreatedId;
    }

    public void setLastCreatedId(Long lastCreatedId) {
        this.lastCreatedId = lastCreatedId;
    }
}