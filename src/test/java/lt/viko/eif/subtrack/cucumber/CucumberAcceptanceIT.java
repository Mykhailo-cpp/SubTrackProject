package lt.viko.eif.subtrack.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit Platform Suite that discovers and runs all Cucumber feature files
 * under {@code src/test/resources/features}.
 *
 * <p>Uses the {@code cucumber} engine declared via {@link IncludeEngines},
 * points the glue at the step-definition packages, and enables pretty
 * console output plus an HTML summary report.</p>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "lt.viko.eif.subtrack.cucumber")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/cucumber.html"
)
public class CucumberAcceptanceIT {
    // JUnit Platform Suite — no body needed.
}
