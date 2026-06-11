package lt.viko.eif.subtrack.config;

import lt.viko.eif.subtrack.service.CurrencyServiceImpl;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * General application configuration.
 *
 * <p>Declares infrastructure beans shared across the application and enables
 * Spring's annotation-driven caching support. Exchange-rate results fetched
 * by {@link CurrencyServiceImpl} are cached under the
 * {@code exchangeRates} cache to limit calls to the external API.</p>
 */
@Configuration
@EnableCaching
public class AppConfig {

    /**
     * Creates the {@link RestTemplate} used for external HTTP calls.
     *
     * <p>Declared as a bean so it can be injected into services and replaced
     * with a mock or test double in unit tests.</p>
     *
     * @return a default {@link RestTemplate} instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}