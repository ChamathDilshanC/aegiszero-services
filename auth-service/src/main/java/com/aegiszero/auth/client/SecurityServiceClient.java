package com.aegiszero.auth.client;

import com.aegiszero.auth.client.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SecurityServiceClient {

    private final RestClient securityServiceRestClient;

    public SecurityServiceClient(RestClient securityServiceRestClient) {
        this.securityServiceRestClient = securityServiceRestClient;
    }

    public RiskEvaluationResponse evaluateRisk(RiskEvaluationRequest request) {
        return securityServiceRestClient.post()
                .uri("/api/security/internal/risk/evaluate")
                .body(request)
                .retrieve()
                .body(RiskEvaluationResponse.class);
    }

    public MfaMethodsResponse getMfaMethods(String userId) {
        return securityServiceRestClient.get()
                .uri("/api/security/internal/users/{userId}/mfa-methods", userId)
                .retrieve()
                .body(MfaMethodsResponse.class);
    }

    public MfaChallengeResponse createChallenge(MfaChallengeRequest request) {
        return securityServiceRestClient.post()
                .uri("/api/security/internal/mfa/challenge")
                .body(request)
                .retrieve()
                .body(MfaChallengeResponse.class);
    }

    public MfaVerifyInternalResponse verifyChallenge(MfaVerifyInternalRequest request) {
        return securityServiceRestClient.post()
                .uri("/api/security/internal/mfa/verify")
                .body(request)
                .retrieve()
                .body(MfaVerifyInternalResponse.class);
    }

    public SessionResponse createSession(SessionCreateRequest request) {
        return securityServiceRestClient.post()
                .uri("/api/security/internal/sessions")
                .body(request)
                .retrieve()
                .body(SessionResponse.class);
    }

    public void revokeSession(String sessionId) {
        securityServiceRestClient.delete()
                .uri("/api/security/internal/sessions/{sessionId}", sessionId)
                .retrieve()
                .toBodilessEntity();
    }
}
