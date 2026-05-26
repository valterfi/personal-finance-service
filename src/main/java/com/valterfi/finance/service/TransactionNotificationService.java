package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valterfi.finance.config.FinanceTransactionInsightProperties;
import com.valterfi.finance.config.TwilioWhatsAppProperties;
import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionNotificationService {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.10");

    private final TwilioService notificationService;
    private final TwilioWhatsAppProperties whatsAppProperties;
    private final FinanceTransactionInsightProperties transactionInsightProperties;
    private final ObjectMapper objectMapper;

    public void sendMessage(Transaction transaction) {
        validateTransactionTemplate();

        String contentVariables = toContentVariables(transaction);
        notificationService.sendMessage(whatsAppProperties.getTransactionTemplateId(), contentVariables);
    }

    public void sendInsightMessage(Transaction transaction, BigDecimal expectedSpendingToday) {
        if (!transactionInsightProperties.isEnabled()) {
            log.debug("Transaction insight message is disabled. Skipping WhatsApp insight notification.");
            return;
        }

        validateTransactionInsightTemplate();

        String contentVariables = toInsightContentVariables(transaction, expectedSpendingToday);
        notificationService.sendMessage(whatsAppProperties.getTransactionInsightTemplateId(), contentVariables);
    }

    private void validateTransactionTemplate() {
        if (!StringUtils.hasText(whatsAppProperties.getTransactionTemplateId())) {
            throw new IllegalStateException("Twilio WhatsApp transaction template id is missing. Configure twilio.whatsapp.transaction-template-id.");
        }
    }

    private void validateTransactionInsightTemplate() {
        if (!StringUtils.hasText(whatsAppProperties.getTransactionInsightTemplateId())) {
            throw new IllegalStateException("Twilio WhatsApp transaction insight template id is missing. Configure twilio.whatsapp.transaction-insight-template-id.");
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

    private String toInsightContentVariables(Transaction transaction, BigDecimal expectedSpendingToday) {
        Statement statement = transaction.getStatement();
        BigDecimal currentBalance = statement.getCurrentBalance();
        BigDecimal targetBalance = statement.getTargetStatementBalance();
        BigDecimal difference = currentBalance.subtract(expectedSpendingToday);
        SpendingPaceInsight insight = resolveSpendingPaceInsight(currentBalance, targetBalance, difference);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", insight.emoji());
        variables.put("2", insight.label());
        variables.put("3", purchaseSummary(transaction));
        variables.put("4", formatWholeAmount(currentBalance));
        variables.put("5", formatWholeAmount(targetBalance));
        variables.put("6", jamesSummary(difference));

        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Twilio WhatsApp insight template variables.", exception);
        }
    }

    /**
     * Resolves the spending pace classification used by the transaction insight WhatsApp template.
     * <p>
     * The current statement balance is compared with the expected spending for the transaction date:
     * {@code difference = currentBalance - expectedSpendingToday}. Spending at or below the expected
     * pace is {@code BOM}. Spending above the expected pace is {@code ATENCAO} while the excess is up
     * to 10% of the monthly target, and {@code ALTO} when it is above that threshold. If the current
     * balance already reached the statement target, the insight is {@code CRITICO}.
     */
    private SpendingPaceInsight resolveSpendingPaceInsight(
            BigDecimal currentBalance,
            BigDecimal targetBalance,
            BigDecimal difference) {
        if (currentBalance.compareTo(targetBalance) >= 0) {
            return new SpendingPaceInsight("🚨", "CRÍTICO");
        }
        if (difference.signum() <= 0) {
            return new SpendingPaceInsight("🟢", "BOM");
        }
        if (targetBalance.signum() == 0) {
            return new SpendingPaceInsight("🚨", "CRÍTICO");
        }

        BigDecimal differenceRatio = difference.divide(targetBalance, 4, RoundingMode.HALF_UP);
        if (differenceRatio.compareTo(WARNING_THRESHOLD) <= 0) {
            return new SpendingPaceInsight("🟡", "ATENÇÃO");
        }
        return new SpendingPaceInsight("🔴", "ALTO");
    }

    private String purchaseSummary(Transaction transaction) {
        return "no cartão final "
                + transaction.getCard()
                + ", no valor de "
                + formatAmount(transaction.getAmount())
                + ", em "
                + transaction.getDate().format(DATE_FORMATTER)
                + " às "
                + transaction.getTime().format(TIME_FORMATTER)
                + ", em "
                + transaction.getDescription()
                + ", foi aprovada";
    }

    private String jamesSummary(BigDecimal difference) {
        if (difference.signum() > 0) {
            return "Você está " + formatWholeAmount(difference) + " acima do esperado hoje.";
        }
        if (difference.signum() < 0) {
            return "Você está " + formatWholeAmount(difference.abs()) + " abaixo do esperado hoje.";
        }
        return "Você está dentro do esperado hoje.";
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(amount);
    }

    private String formatWholeAmount(BigDecimal amount) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(BRAZIL);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    private record SpendingPaceInsight(String emoji, String label) {
    }
}
