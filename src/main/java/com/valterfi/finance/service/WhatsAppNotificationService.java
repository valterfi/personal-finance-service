package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.valterfi.finance.config.TwilioWhatsAppProperties;
import com.valterfi.finance.model.Transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationService {

    private static final String WHATSAPP_PREFIX = "whatsapp:";
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TwilioWhatsAppProperties properties;
    private final ObjectMapper objectMapper;

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

    public void sendMessage(Transaction transaction) {
        validateConfiguration();
        validateRecipients();
        validateTransactionTemplate();

        Twilio.init(properties.getAccountSid(), properties.getAuthToken());
        String contentVariables = toContentVariables(transaction);

        properties.getToNumbers().forEach(toMobile -> {
            try {
                Message message = Message.creator(
                        new PhoneNumber(toWhatsAppNumber(toMobile)),
                        new PhoneNumber(toWhatsAppNumber(properties.getFromNumber())),
                        (String) null)
                        .setContentSid(properties.getTransactionTemplateId())
                        .setContentVariables(contentVariables)
                        .create();

                log.info("WhatsApp transaction template sent successfully. sid={}, status={}, transactionId={}",
                        message.getSid(), message.getStatus(), transaction.getId());
            } catch (Exception exception) {
                log.error("Failed to send WhatsApp transaction template to {}. transactionId={}",
                        toMobile, transaction.getId(), exception);
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

    private void validateTransactionTemplate() {
        if (!StringUtils.hasText(properties.getTransactionTemplateId())) {
            throw new IllegalStateException("Twilio WhatsApp transaction template id is missing. Configure twilio.whatsapp.transaction-template-id.");
        }
    }

    private String toContentVariables(Transaction transaction) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", transaction.getCard());
        variables.put("2", formatAmount(transaction.getAmount()));
        variables.put("3", transaction.getDate().format(DATE_FORMATTER));
        variables.put("4", transaction.getTime().format(TIME_FORMATTER));
        variables.put("5", transaction.getDescription());

        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Twilio WhatsApp template variables.", exception);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(amount);
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
