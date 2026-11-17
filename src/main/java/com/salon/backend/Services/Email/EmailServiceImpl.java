package com.salon.backend.Services.Email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ApplicationEventPublisher eventPublisher;
    private final EmailDispatcher dispatcher;

    @Override
    public void queue(String to, String recipientName, EmailTemplate template) {
        if (!shouldSend(to, template)) return;
        // Delivered by EmailDispatcher after commit (or at once with no transaction).
        eventPublisher.publishEvent(new EmailRequestedEvent(to.trim(), recipientName, template));
    }

    @Override
    public boolean sendNow(String to, String recipientName, EmailTemplate template) {
        if (!shouldSend(to, template)) return false;
        return dispatcher.deliver(new EmailRequestedEvent(to.trim(), recipientName, template));
    }

    private boolean shouldSend(String to, EmailTemplate template) {
        return template != null
                && template.priority().sendsEmail()
                && to != null
                && !to.isBlank();
    }
}
