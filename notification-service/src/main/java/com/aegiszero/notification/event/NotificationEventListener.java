package com.aegiszero.notification.event;

import com.aegiszero.common.event.EventTopics;
import com.aegiszero.common.event.NotificationEvent;
import com.aegiszero.common.messaging.EventSubscription;
import com.aegiszero.notification.service.EmailService;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener implements EventSubscription<NotificationEvent> {

    private final EmailService emailService;

    public NotificationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String topic() {
        return EventTopics.NOTIFICATION_EVENTS;
    }

    @Override
    public Class<NotificationEvent> payloadType() {
        return NotificationEvent.class;
    }

    @Override
    public void handle(NotificationEvent event) {
        emailService.send(event);
    }
}
