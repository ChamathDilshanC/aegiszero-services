package com.aegiszero.notification.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fails startup with a clear message if {@code email.provider} is set to
 * anything other than a known value, instead of letting it silently fall
 * through to a confusing "no EmailSender bean found" wiring error.
 */
@Component
public class EmailProviderValidator {

    private static final List<String> SUPPORTED_PROVIDERS = List.of("smtp", "brevo-api");

    private final String provider;

    public EmailProviderValidator(@Value("${email.provider:smtp}") String provider) {
        this.provider = provider;
    }

    @PostConstruct
    public void validate() {
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            throw new IllegalStateException(
                    "Unsupported email.provider '%s' — expected one of %s"
                            .formatted(provider, SUPPORTED_PROVIDERS));
        }
    }
}
