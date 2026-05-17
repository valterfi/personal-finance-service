package com.valterfi.finance.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "finance.expense-analysis.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExpenseAnalysisScheduler {

    private final ExpenseAnalysisService expenseAnalysisService;
    private final WhatsAppNotificationService notificationService;

    @Scheduled(cron = "0 0 15,21 * * *", zone = "America/Sao_Paulo")
    public void sendDailyAnalysis() {
        try {
            String analysis = expenseAnalysisService.analyzeTodayExpenses();
            notificationService.sendMessage(analysis);
            log.info("Current date expense analysis: {}", analysis);
        } catch (Exception e) {
            log.error("Failed to generate/send expense analysis", e);
        }
    }
}
