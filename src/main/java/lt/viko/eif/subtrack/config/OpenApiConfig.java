package lt.viko.eif.subtrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for SubTrack API documentation.
 *
 * <p>Registers the global JWT bearer-token security scheme so that every
 * secured endpoint shows an "Authorize" button in Swagger UI, and adds
 * server entries for local development.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI subTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SubTrack API")
                        .description("""
                                RESTful Web Service for tracking paid subscriptions.

                                ## Authentication
                                Most endpoints require a **JWT bearer token**.  \s
                                1. Call `POST /api/auth/register` or `POST /api/auth/login` to obtain a token.
                                2. Click **Authorize** (top-right) and enter `Bearer <your-token>`.

                                ## HATEOAS
                                Subscription responses include hypermedia links (`self`, `subscriptions`) \
                                that satisfy Richardson Maturity Model Level 3.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SubTrack Team")
                                .email("subtrack@example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token returned by /api/auth/login. " +
                                                "Format: `Bearer <token>` (the prefix is added automatically).")));
    }
}