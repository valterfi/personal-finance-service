package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
        variables.put("4", bold(formatAmount(currentBalance)));
        variables.put("5", bold(formatAmount(targetBalance)));
        variables.put("6", jamesSummary(difference, currentBalance, targetBalance, transaction.getDate(), statement));

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
                + bold(transaction.getCard())
                + ", no valor de "
                + bold(formatAmount(transaction.getAmount()))
                + ", em "
                + bold(transaction.getDate().format(DATE_FORMATTER))
                + " às "
                + bold(transaction.getTime().format(TIME_FORMATTER))
                + ", em "
                + bold(transaction.getDescription())
                + ", foi aprovada";
    }

    private String jamesSummary(
            BigDecimal difference,
            BigDecimal currentBalance,
            BigDecimal targetBalance,
            LocalDate transactionDate,
            Statement statement) {
        String targetSummary = targetSummary(currentBalance, targetBalance);
        String dailyPaceSummary = dailyPaceSummary(currentBalance, targetBalance, transactionDate, statement);

        if (difference.signum() > 0) {
            return "Você está " + bold(formatAmount(difference)) + " acima do esperado hoje. " + targetSummary + ". " + dailyPaceSummary;
        }
        if (difference.signum() < 0) {
            return "Você está " + bold(formatAmount(difference.abs())) + " abaixo do esperado hoje. " + targetSummary + ". " + dailyPaceSummary;
        }
        return "Você está dentro do esperado hoje. " + targetSummary + ". " + dailyPaceSummary;
    }

    private String targetSummary(BigDecimal currentBalance, BigDecimal targetBalance) {
        if (currentBalance.compareTo(targetBalance) > 0) {
            return "Você ultrapassou a meta do ciclo em " + bold(formatAmount(currentBalance.subtract(targetBalance)));
        }

        BigDecimal missingToTarget = targetBalance.subtract(currentBalance);
        return "Faltam apenas " + bold(formatAmount(missingToTarget)) + " para atingir a meta do ciclo";
    }

    private String dailyPaceSummary(
            BigDecimal currentBalance,
            BigDecimal targetBalance,
            LocalDate transactionDate,
            Statement statement) {
        long currentCycleDay = Math.max(1, ChronoUnit.DAYS.between(statement.getStartDate(), transactionDate) + 1);
        long remainingCycleDays = Math.max(0, ChronoUnit.DAYS.between(transactionDate, statement.getClosingDate()));
        BigDecimal missingToTarget = targetBalance.subtract(currentBalance).max(BigDecimal.ZERO);
        BigDecimal averageDailySpending = currentBalance.divide(BigDecimal.valueOf(currentCycleDay), 2, RoundingMode.HALF_UP);
        BigDecimal recommendedDailyLimit = remainingCycleDays == 0
                ? BigDecimal.ZERO
                : missingToTarget.divide(BigDecimal.valueOf(remainingCycleDays), 2, RoundingMode.HALF_UP);

        return "Você está gastando em média "
                + bold(formatAmount(averageDailySpending) + "/dia")
                + " no ciclo. Para terminar dentro da meta, o ideal agora é manter os próximos dias em até "
                + bold(formatAmount(recommendedDailyLimit) + "/dia");
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(amount);
    }

    private String bold(String value) {
        return "*" + value + "*";
    }

    private record SpendingPaceInsight(String emoji, String label) {
    }
}
