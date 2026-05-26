package com.valterfi.finance.service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valterfi.finance.config.TwilioWhatsAppProperties;
import com.valterfi.finance.util.BrazilDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "finance.expense-analysis.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExpenseAnalysisScheduler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExpenseAnalysisService expenseAnalysisService;
    private final TwilioService notificationService;
    private final TwilioWhatsAppProperties whatsAppProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 15,22 * * *", zone = "America/Sao_Paulo")
    public void sendDailyAnalysis() {
        try {
            String analysis = expenseAnalysisService.analyzeTodayExpenses();
            String contentVariables = toContentVariables(analysis);
            notificationService.sendMessage(getDailyAnalysisTemplateId(), contentVariables);
            log.info("Current date expense analysis: {}", analysis);
        } catch (Exception e) {
            log.error("Failed to generate/send expense analysis", e);
        }
    }

    private String getDailyAnalysisTemplateId() {
        String templateId = whatsAppProperties.getDailyAnalysisTemplateId();
        if (!StringUtils.hasText(templateId)) {
            throw new IllegalStateException("Twilio WhatsApp daily analysis template id is missing. Configure twilio.whatsapp.daily-analysis-template-id.");
        }
        return templateId;
    }

    private String toContentVariables(String analysis) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("1", BrazilDateTime.today().format(DATE_FORMATTER));
        variables.put("2", toTemplateVariableValue(analysis));

        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Twilio WhatsApp daily analysis template variables.", exception);
        }
    }

    private String toTemplateVariableValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "No analysis available.";
        }

        return value.replaceAll("\\s+", " ").trim();
    }
}
