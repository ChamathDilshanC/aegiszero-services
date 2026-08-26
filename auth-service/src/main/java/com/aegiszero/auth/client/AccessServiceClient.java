package com.aegiszero.auth.client;

import com.aegiszero.auth.client.dto.AssignRoleRequest;
import com.aegiszero.auth.client.dto.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AccessServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccessServiceClient.class);

    private final RestClient accessServiceRestClient;

    public AccessServiceClient(RestClient accessServiceRestClient) {
        this.accessServiceRestClient = accessServiceRestClient;
    }

    /**
     * Fetches the caller's roles and permissions. Falls back to an empty
     * authorization set if access-service is unreachable so that auth-service
     * degrades to "least privilege" rather than failing the whole login.
     */
    public AuthorizationResponse getAuthorization(UUID userId) {
        try {
            return accessServiceRestClient.get()
                    .uri("/api/access/internal/users/{userId}/authorization", userId)
                    .retrieve()
                    .body(AuthorizationResponse.class);
        } catch (Exception ex) {
            log.warn("access-service unreachable while resolving authorization for {}: {}", userId, ex.getMessage());
            return AuthorizationResponse.empty();
        }
    }

    /**
     * Grants a role with no logged-in actor behind it — used when an admin
     * access request is approved from an email link, where there is no JWT
     * session to carry ROLE_ASSIGN. Throws on failure (unlike
     * {@link #getAuthorization}) since the caller needs to know the grant
     * didn't happen rather than silently proceeding as if it had.
     */
    public void assignRole(UUID userId, String roleName) {
        accessServiceRestClient.post()
                .uri("/api/access/internal/users/{userId}/roles", userId)
                .body(new AssignRoleRequest(roleName))
                .retrieve()
                .toBodilessEntity();
    }
}
