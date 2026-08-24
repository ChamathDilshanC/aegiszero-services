package com.aegiszero.auth.event;

import com.aegiszero.auth.service.TokenService;
import com.aegiszero.common.event.EventTopics;
import com.aegiszero.common.event.SessionRevokedEvent;
import com.aegiszero.common.messaging.EventSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Closes the loop when security-service revokes a session (explicit user
 * action or an admin blocking a device): the refresh tokens tied to that
 * session must stop working too, or "revoke this session" would be cosmetic.
 */
@Component
public class SessionRevokedEventListener implements EventSubscription<SessionRevokedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SessionRevokedEventListener.class);

    private final TokenService tokenService;

    public SessionRevokedEventListener(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public String topic() {
        return EventTopics.SESSION_REVOKED;
    }

    @Override
    public Class<SessionRevokedEvent> payloadType() {
        return SessionRevokedEvent.class;
    }

    @Override
    public void handle(SessionRevokedEvent event) {
        log.info("Revoking refresh tokens for session {} (reason: {})", event.sessionId(), event.reason());
        tokenService.revokeAllForSession(UUID.fromString(event.sessionId()));
    }
}
