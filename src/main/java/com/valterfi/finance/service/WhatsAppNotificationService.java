package com.valterfi.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.valterfi.finance.config.TwilioWhatsAppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationService {

    private static final String WHATSAPP_PREFIX = "whatsapp:";

    private final TwilioWhatsAppProperties properties;

    public void sendMessage(String text) {
        validateConfiguration();
        validateRecipients();

        Twilio.init(properties.getAccountSid(), properties.getAuthToken());

        properties.getToNumbers().forEach(toMobile -> {
            try {
                Message message = Message.creator(
                        new PhoneNumber(toWhatsAppNumber(toMobile)),
                        new PhoneNumber(toWhatsAppNumber(properties.getFromNumber())),
                        text)
                        .create();

                log.info("WhatsApp message sent successfully. sid={}, status={}", message.getSid(), message.getStatus());
            } catch (Exception exception) {
                log.error("Failed to send WhatsApp message to {}", toMobile, exception);
                throw exception;
            }
        });
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getAccountSid())
                || !StringUtils.hasText(properties.getAuthToken())
                || !StringUtils.hasText(properties.getFromNumber())) {
            throw new IllegalStateException("Twilio WhatsApp configuration is missing. Configure twilio.whatsapp account-sid, auth-token and from-number.");
        }
    }

    private void validateRecipients() {
        if (properties.getToNumbers().isEmpty()) {
            throw new IllegalStateException("Twilio WhatsApp recipients are missing. Configure twilio.whatsapp.to-numbers.");
        }
    }

    private String toWhatsAppNumber(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            throw new IllegalArgumentException("WhatsApp mobile number must not be blank.");
        }

        String normalizedMobile = mobile.trim();
        if (normalizedMobile.startsWith(WHATSAPP_PREFIX)) {
            return normalizedMobile;
        }

        return WHATSAPP_PREFIX + normalizedMobile;
    }

}
