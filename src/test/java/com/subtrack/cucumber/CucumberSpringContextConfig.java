package com.subtrack.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Binds the Cucumber step-definition context to a full Spring Boot
 * integration-test context backed by the H2 in-memory database.
 *
 * <p>{@link AutoConfigureMockMvc} wires up a {@code MockMvc} instance
 * that is shared across all step-definition classes via Spring's
 * dependency injection; no real HTTP port is opened.</p>
 *
 * <p>The {@code test} profile activates {@code application.properties}
 * under {@code src/test/resources}, which points to H2 and disables
 * Liquibase in favour of {@code spring.jpa.hibernate.ddl-auto=create-drop}.</p>
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringContextConfig {
    // no body — purely a configuration anchor
}
