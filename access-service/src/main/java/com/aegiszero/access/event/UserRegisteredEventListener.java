package com.aegiszero.access.event;

import com.aegiszero.access.service.AuthorizationQueryService;
import com.aegiszero.common.event.EventTopics;
import com.aegiszero.common.event.UserRegisteredEvent;
import com.aegiszero.common.messaging.EventSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserRegisteredEventListener implements EventSubscription<UserRegisteredEvent> {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);

    private final AuthorizationQueryService authorizationQueryService;

    public UserRegisteredEventListener(AuthorizationQueryService authorizationQueryService) {
        this.authorizationQueryService = authorizationQueryService;
    }

    @Override
    public String topic() {
        return EventTopics.USER_REGISTERED;
    }

    @Override
    public Class<UserRegisteredEvent> payloadType() {
        return UserRegisteredEvent.class;
    }

    @Override
    public void handle(UserRegisteredEvent event) {
        log.info("Assigning default role to newly registered user {}", event.userId());
        authorizationQueryService.assignDefaultRoleIfMissing(UUID.fromString(event.userId()));
    }
}
