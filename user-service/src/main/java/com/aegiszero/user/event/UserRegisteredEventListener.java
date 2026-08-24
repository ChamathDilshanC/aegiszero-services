package com.aegiszero.user.event;

import com.aegiszero.common.event.EventTopics;
import com.aegiszero.common.event.UserRegisteredEvent;
import com.aegiszero.common.messaging.EventSubscription;
import com.aegiszero.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserRegisteredEventListener implements EventSubscription<UserRegisteredEvent> {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);

    private final UserService userService;

    public UserRegisteredEventListener(UserService userService) {
        this.userService = userService;
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
        log.info("Creating profile for newly registered user {}", event.userId());
        userService.createFromRegistration(UUID.fromString(event.userId()), event.email(),
                event.firstName(), event.lastName());
    }
}
