package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valterfi.finance.config.FinanceTransactionInsightProperties;
import com.valterfi.finance.config.TwilioWhatsAppProperties;
import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;

class TransactionNotificationServiceTest {

    private StubWhatsAppNotificationService notificationService;
    private TwilioWhatsAppProperties whatsAppProperties;
    private FinanceTransactionInsightProperties transactionInsightProperties;
    private TransactionNotificationService transactionNotificationService;

    @BeforeEach
    void setUp() {
        notificationService = new StubWhatsAppNotificationService();
        whatsAppProperties = new TwilioWhatsAppProperties();
        whatsAppProperties.setTransactionTemplateId("HX_TRANSACTION_TEMPLATE");
        whatsAppProperties.setTransactionInsightTemplateId("HX_TRANSACTION_INSIGHT_TEMPLATE");
        transactionInsightProperties = new FinanceTransactionInsightProperties();
        transactionNotificationService = new TransactionNotificationService(
                notificationService,
                whatsAppProperties,
                transactionInsightProperties,
                new ObjectMapper());
    }

    @Test
    void shouldSendTransactionMessage() throws Exception {
        Transaction transaction = transaction();

        transactionNotificationService.sendMessage(transaction);

        MessageRequest message = notificationService.messages.getFirst();
        Map<?, ?> variables = new ObjectMapper().readValue(message.contentVariables(), Map.class);
        Assertions.assertEquals("HX_TRANSACTION_TEMPLATE", message.contentSid());
        Assertions.assertEquals("7396", variables.get("1"));
        Assertions.assertTrue(String.valueOf(variables.get("2")).contains("202,44"));
        Assertions.assertEquals("25/05/2026", variables.get("3"));
        Assertions.assertEquals("13:59", variables.get("4"));
        Assertions.assertEquals("CARREFOUR TTE 43 SAO PAULO BRA", variables.get("5"));
    }

    @Test
    void shouldSendInsightMessageWhenEnabled() throws Exception {
        Transaction transaction = transaction();
        transaction.setCard("5149");
        transaction.setAmount(new BigDecimal("15.00"));
        transaction.setTime(LocalTime.of(10, 12));
        transaction.setDescription("W FEIJO SAO PAULO BRA");
        transaction.setStatement(statement(new BigDecimal("5200.00")));
        transactionInsightProperties.setEnabled(true);

        transactionNotificationService.sendInsightMessage(transaction, new BigDecimal("4666.67"));

        MessageRequest insightMessage = notificationService.messages.getFirst();
        Map<?, ?> variables = new ObjectMapper().readValue(insightMessage.contentVariables(), Map.class);
        Assertions.assertEquals("HX_TRANSACTION_INSIGHT_TEMPLATE", insightMessage.contentSid());
        Assertions.assertEquals("🟡", variables.get("1"));
        Assertions.assertEquals("ATENÇÃO", variables.get("2"));
        Assertions.assertTrue(String.valueOf(variables.get("3")).contains("no cartão final 5149"));
        Assertions.assertTrue(String.valueOf(variables.get("3")).contains("no valor de R$"));
        Assertions.assertTrue(String.valueOf(variables.get("3")).contains("15,00"));
        Assertions.assertTrue(String.valueOf(variables.get("3")).contains("25/05/2026 às 10:12"));
        Assertions.assertTrue(String.valueOf(variables.get("3")).contains("W FEIJO SAO PAULO BRA"));
        Assertions.assertTrue(String.valueOf(variables.get("4")).contains("R$"));
        Assertions.assertTrue(String.valueOf(variables.get("4")).contains("5.200"));
        Assertions.assertTrue(String.valueOf(variables.get("5")).contains("R$"));
        Assertions.assertTrue(String.valueOf(variables.get("5")).contains("14.000"));
        Assertions.assertTrue(String.valueOf(variables.get("6")).contains("Você está R$"));
        Assertions.assertTrue(String.valueOf(variables.get("6")).contains("533"));
        Assertions.assertTrue(String.valueOf(variables.get("6")).contains("acima do esperado hoje."));
    }

    @Test
    void shouldNotSendInsightMessageWhenDisabled() {
        Transaction transaction = transaction();
        transaction.setStatement(statement(new BigDecimal("5200.00")));

        transactionNotificationService.sendInsightMessage(transaction, new BigDecimal("4666.67"));

        Assertions.assertTrue(notificationService.messages.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "4000.00, 14000.00, 4666.67, 🟢, BOM",
            "4666.67, 14000.00, 4666.67, 🟢, BOM",
            "5200.00, 14000.00, 4666.67, 🟡, ATENÇÃO",
            "6200.00, 14000.00, 4666.67, 🔴, ALTO",
            "14000.00, 14000.00, 4666.67, 🚨, CRÍTICO",
            "15000.00, 14000.00, 4666.67, 🚨, CRÍTICO",
            "-1.00, 0.00, -2.00, 🚨, CRÍTICO"
    })
    void shouldResolveAllSpendingPaceInsightCombinations(
            BigDecimal currentBalance,
            BigDecimal targetBalance,
            BigDecimal expectedSpendingToday,
            String expectedEmoji,
            String expectedLabel) throws Exception {
        Transaction transaction = transaction();
        transaction.setStatement(statement(currentBalance, targetBalance));
        transactionInsightProperties.setEnabled(true);

        transactionNotificationService.sendInsightMessage(transaction, expectedSpendingToday);

        MessageRequest insightMessage = notificationService.messages.getFirst();
        Map<?, ?> variables = new ObjectMapper().readValue(insightMessage.contentVariables(), Map.class);
        Assertions.assertEquals(expectedEmoji, variables.get("1"));
        Assertions.assertEquals(expectedLabel, variables.get("2"));
    }

    private Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.setDate(LocalDate.of(2026, 5, 25));
        transaction.setTime(LocalTime.of(13, 59));
        transaction.setDescription("CARREFOUR TTE 43 SAO PAULO BRA");
        transaction.setAmount(new BigDecimal("202.44"));
        transaction.setCard("7396");
        return transaction;
    }

    private Statement statement(BigDecimal currentBalance) {
        return statement(currentBalance, new BigDecimal("14000.00"));
    }

    private Statement statement(BigDecimal currentBalance, BigDecimal targetBalance) {
        Statement statement = new Statement();
        statement.setTargetStatementBalance(targetBalance);
        statement.setCurrentBalance(currentBalance);
        return statement;
    }

    private static class StubWhatsAppNotificationService extends TwilioService {

        private final List<MessageRequest> messages = new ArrayList<>();

        private StubWhatsAppNotificationService() {
            super(new TwilioWhatsAppProperties());
        }

        @Override
        public void sendMessage(String contentSid, String contentVariables) {
            messages.add(new MessageRequest(contentSid, contentVariables));
        }
    }

    private record MessageRequest(String contentSid, String contentVariables) {
    }
}
