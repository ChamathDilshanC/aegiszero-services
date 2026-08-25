package com.aegiszero.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot contributes a MailHealthIndicator whenever a JavaMailSenderImpl
 * bean exists, and it opens a real SMTP connection every time /actuator/health
 * is polled. That is the path Render health-checks, so on a host where outbound
 * SMTP is blocked - and with email.provider=brevo-api, where SMTP is not used at
 * all - a perfectly healthy service reports DOWN with
 * {@code SMTPTransport.openServer ... ConnectException: Connection refused} and
 * the deploy fails.
 *
 * <p>application.yml disables it. The risk being covered here is a silent one: a
 * typo in the property name would leave the indicator active and break the
 * deploy again with nothing pointing at the cause. So these pin both halves -
 * that the indicator really would be registered otherwise, and that this exact
 * property is what turns it off.
 */
class MailHealthIndicatorDisabledTest {

    private static final String MAIL_HEALTH_BEAN = "mailHealthContributor";
    private static final String DISABLE_PROPERTY = "management.health.mail.enabled";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailHealthContributorAutoConfiguration.class))
            .withBean(JavaMailSenderImpl.class, JavaMailSenderImpl::new);

    @Test
    void springBootRegistersTheMailHealthIndicatorByDefault() {
        runner.run(context -> assertThat(context).hasBean(MAIL_HEALTH_BEAN));
    }

    @Test
    void thePropertyNameWeRelyOnActuallyDisablesIt() {
        runner.withPropertyValues(DISABLE_PROPERTY + "=false")
                .run(context -> assertThat(context).doesNotHaveBean(MAIL_HEALTH_BEAN));
    }

    @Test
    void applicationYmlSetsThatProperty() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"));

        Object value = sources.stream()
                .map(source -> source.getProperty(DISABLE_PROPERTY))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);

        assertThat(value)
                .as("%s must be present in application.yml, or /actuator/health "
                        + "opens an SMTP connection and Render marks the service unhealthy",
                        DISABLE_PROPERTY)
                .isNotNull();
        assertThat(String.valueOf(value)).isEqualTo("false");
    }
}
