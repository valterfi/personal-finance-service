package com.valterfi.finance.util;

import java.math.BigDecimal;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMultipart;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class MessageUtils {

    public static String extractBody(Message message) throws Exception {
        MimeMultipart content = (MimeMultipart) message.getContent();
        Object bodyContent = content.getBodyPart(0).getContent();
        return normalizeWhitespace(bodyContent == null ? "" : String.valueOf(bodyContent));
    }

    public static BigDecimal parseAmount(String amount) {
        return new BigDecimal(amount.replace(".", "").replace(",", "."));
    }

    public static String normalizeWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    public static String safeSubject(Message message) {
        try {
            return message.getSubject();
        } catch (Exception exception) {
            return "[unavailable]";
        }
    }
}
