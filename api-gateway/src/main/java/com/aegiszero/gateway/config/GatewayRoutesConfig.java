package com.aegiszero.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                                RedisRateLimiter authRateLimiter,
                                KeyResolver ipKeyResolver,
                                @Value("${aegiszero.services.auth-service-url}") String authServiceUrl,
                                @Value("${aegiszero.services.user-service-url}") String userServiceUrl,
                                @Value("${aegiszero.services.access-service-url}") String accessServiceUrl,
                                @Value("${aegiszero.services.security-service-url}") String securityServiceUrl,
                                @Value("${aegiszero.services.audit-service-url}") String auditServiceUrl) {
        return builder.routes()
                // Rate-limited, security-sensitive auth endpoints.
                .route("auth-login", r -> r.path("/api/auth/login", "/api/auth/register", "/api/auth/forgot-password")
                        .filters(f -> f.requestRateLimiter(rl -> rl.setRateLimiter(authRateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri(authServiceUrl))
                .route("mfa-rate-limited", r -> r.path("/api/security/mfa/**")
                        .filters(f -> f.requestRateLimiter(rl -> rl.setRateLimiter(authRateLimiter).setKeyResolver(ipKeyResolver)))
                        .uri(securityServiceUrl))

                // General service routes.
                .route("auth-service", r -> r.path("/api/auth/**").uri(authServiceUrl))
                .route("user-service", r -> r.path("/api/users/**").uri(userServiceUrl))
                .route("access-service", r -> r.path("/api/access/**").uri(accessServiceUrl))
                .route("security-service", r -> r.path("/api/security/**").uri(securityServiceUrl))
                .route("audit-service", r -> r.path("/api/audit/**").uri(auditServiceUrl))
                .build();
    }
}
