package com.aegiszero.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.PropertyPlaceholderHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every downstream URL has to survive Render renaming a service.
 *
 * <p>Render appends a suffix when the plain service name is already taken, so
 * aegiszero-auth-service becomes aegiszero-auth-service-y415. A hardcoded
 * http://aegiszero-auth-service:8081 then points at a host that does not
 * resolve, and every request through the gateway comes back as a 500 with
 * nothing in it to suggest a naming problem. The fix is for render.yaml to read
 * host:port off the service itself, which means the property in application.yml
 * has to accept that form as well as a full URL.
 *
 * <p>These assert the placeholder expression exactly as written in
 * application.yml, resolved the way Spring resolves it: a full URL wins, a bare
 * host:port gets the scheme prefixed, and neither present falls back to the
 * local port.
 */
class ServiceUrlFallbackTest {

    private static final PropertyPlaceholderHelper HELPER =
            new PropertyPlaceholderHelper("${", "}", ":", false);

    private final Map<String, Object> applicationYml = loadApplicationYml();

    private String resolve(String propertyKey, Map<String, String> environment) {
        Object raw = applicationYml.get(propertyKey);
        assertThat(raw).as("%s must exist in application.yml", propertyKey).isNotNull();
        return HELPER.replacePlaceholders(String.valueOf(raw), environment::get);
    }

    @Test
    void fallsBackToTheLocalPortWhenNothingIsSet() {
        assertThat(resolve("aegiszero.services.auth-service-url", Map.of()))
                .isEqualTo("http://localhost:8081");
        assertThat(resolve("aegiszero.services.audit-service-url", Map.of()))
                .isEqualTo("http://localhost:8085");
    }

    @Test
    void prefixesTheSchemeOntoABareHostPort() {
        String resolved = resolve("aegiszero.services.auth-service-url",
                Map.of("AUTH_SERVICE_HOSTPORT", "aegiszero-auth-service-y415:8081"));
        assertThat(resolved).isEqualTo("http://aegiszero-auth-service-y415:8081");
    }

    @Test
    void aFullUrlStillWins() {
        String resolved = resolve("aegiszero.services.auth-service-url",
                Map.of("AUTH_SERVICE_URL", "http://auth-service:8081",
                        "AUTH_SERVICE_HOSTPORT", "ignored:9999"));
        assertThat(resolved).isEqualTo("http://auth-service:8081");
    }

    @Test
    void everyDownstreamServiceSupportsBothForms() {
        Map<String, String> ports = Map.of(
                "auth", "8081", "user", "8082", "access", "8083",
                "security", "8084", "audit", "8085");

        ports.forEach((name, port) -> {
            String key = "aegiszero.services." + name + "-service-url";
            String envVar = name.toUpperCase() + "_SERVICE_HOSTPORT";

            assertThat(resolve(key, Map.of()))
                    .as("%s default", key)
                    .isEqualTo("http://localhost:" + port);
            assertThat(resolve(key, Map.of(envVar, "renamed-host:" + port)))
                    .as("%s via %s", key, envVar)
                    .isEqualTo("http://renamed-host:" + port);
        });
    }

    private static Map<String, Object> loadApplicationYml() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application.yml", new ClassPathResource("application.yml"));
            Map<String, Object> flat = new HashMap<>();
            for (PropertySource<?> source : sources) {
                if (source instanceof org.springframework.core.env.EnumerablePropertySource<?> enumerable) {
                    for (String name : enumerable.getPropertyNames()) {
                        flat.put(name, enumerable.getProperty(name));
                    }
                }
            }
            return flat;
        } catch (Exception ex) {
            throw new IllegalStateException("could not read application.yml", ex);
        }
    }
}
