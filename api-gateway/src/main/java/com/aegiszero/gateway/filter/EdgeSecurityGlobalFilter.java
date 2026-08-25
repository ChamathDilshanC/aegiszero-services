package com.aegiszero.gateway.filter;

import com.aegiszero.gateway.security.GatewayJwtValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Runs before routing: blocks direct external access to service-internal
 * endpoints, stamps a correlation id, and rejects requests to protected
 * routes that don't carry a well-formed, unexpired JWT. Actual authorization
 * (permissions) is still enforced by each downstream service.
 */
@Component
public class EdgeSecurityGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final Pattern INTERNAL_PATH = Pattern.compile("^/api/[^/]+/internal(/.*)?$");

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/mfa/verify",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-email"
    );

    private final GatewayJwtValidator jwtValidator;

    public EdgeSecurityGlobalFilter(GatewayJwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        if (INTERNAL_PATH.matcher(path).matches()) {
            return reject(exchange, HttpStatus.NOT_FOUND, "Not found");
        }

        String correlationId = firstHeader(request, CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        if (PUBLIC_PATHS.contains(path) || request.getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        String token = bearerToken(request);
        if (token == null || !jwtValidator.isValid(token)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid access token");
        }

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String bearerToken(ServerHttpRequest request) {
        String header = firstHeader(request, "Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String firstHeader(ServerHttpRequest request, String name) {
        List<String> values = request.getHeaders().get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}"
                .formatted(status.value(), status.getReasonPhrase(), message);
        var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
