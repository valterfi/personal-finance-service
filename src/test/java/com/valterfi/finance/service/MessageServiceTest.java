package com.valterfi.finance.service;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valterfi.finance.config.FinanceStatementProperties;
import com.valterfi.finance.config.FinanceTransactionInsightProperties;
import com.valterfi.finance.config.TwilioWhatsAppProperties;
import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.repository.TransactionRepository;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

class MessageServiceTest {

    private StubMessageParser messageParser;
    private StubStatementService statementService;
    private StubSpendingPaceService spendingPaceService;
    private StubWhatsAppNotificationService notificationService;
    private TransactionRepository transactionRepository;
    private FinanceTransactionInsightProperties transactionInsightProperties;
    private Transaction savedTransaction;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageParser = new StubMessageParser();
        statementService = new StubStatementService();
        spendingPaceService = new StubSpendingPaceService();
        notificationService = new StubWhatsAppNotificationService();
        transactionRepository = transactionRepository();
        transactionInsightProperties = new FinanceTransactionInsightProperties();

        TwilioWhatsAppProperties whatsAppProperties = new TwilioWhatsAppProperties();
        whatsAppProperties.setTransactionTemplateId("HX_TRANSACTION_TEMPLATE");
        whatsAppProperties.setTransactionInsightTemplateId("HX_TRANSACTION_INSIGHT_TEMPLATE");
        messageService = new MessageService(
                messageParser,
                transactionRepository,
                statementService,
                spendingPaceService,
                notificationService,
                whatsAppProperties,
                transactionInsightProperties,
                new ObjectMapper());
    }

    @Test
    void shouldAttachStatementAndPersistParsedTransaction() throws Exception {
        String body = "transaction email body";
        Transaction transaction = transaction();
        Statement statement = new Statement();
        statement.setId(99L);
        statement.setStartDate(LocalDate.of(2026, 5, 16));
        statement.setClosingDate(LocalDate.of(2026, 6, 14));
        statement.setTargetStatementBalance(new BigDecimal("14000.00"));
        statement.setCurrentBalance(new BigDecimal("202.44"));

        MimeMessage message = mimeMessage(body);
        messageParser.transaction = transaction;
        statementService.statement = statement;

        messageService.process(message);

        Assertions.assertEquals(body, messageParser.body);
        Assertions.assertEquals(1, notificationService.messages.size());
        Assertions.assertEquals("HX_TRANSACTION_TEMPLATE", notificationService.messages.get(0).contentSid());
        Assertions.assertTrue(notificationService.messages.get(0).contentVariables().contains("\"1\":\"7396\""));
        Assertions.assertEquals(LocalDate.of(2026, 5, 25), statementService.transactionDate);
        Assertions.assertSame(statement, savedTransaction.getStatement());
        Assertions.assertEquals(123L, savedTransaction.getId());
        Assertions.assertSame(savedTransaction, statementService.balanceTransaction);
        Assertions.assertSame(savedTransaction, spendingPaceService.transaction);
        Assertions.assertSame(statement, spendingPaceService.statement);
    }

    @Test
    void shouldNotPersistWhenBodyDoesNotMatchTransactionPattern() throws Exception {
        String body = "random email body";

        MimeMessage message = mimeMessage(body);

        messageService.process(message);

        Assertions.assertEquals(body, messageParser.body);
        Assertions.assertNull(statementService.transactionDate);
        Assertions.assertNull(statementService.balanceTransaction);
        Assertions.assertNull(spendingPaceService.transaction);
        Assertions.assertTrue(notificationService.messages.isEmpty());
        Assertions.assertNull(savedTransaction);
    }

    @Test
    void shouldSendInsightMessageAfterPersistingTransaction() throws Exception {
        String body = "transaction email body";
        Transaction transaction = transaction();
        transaction.setCard("5149");
        transaction.setAmount(new BigDecimal("15.00"));
        transaction.setTime(LocalTime.of(10, 12));
        transaction.setDescription("W FEIJO SAO PAULO BRA");

        Statement statement = new Statement();
        statement.setStartDate(LocalDate.of(2026, 5, 16));
        statement.setClosingDate(LocalDate.of(2026, 6, 14));
        statement.setTargetStatementBalance(new BigDecimal("14000.00"));
        statement.setCurrentBalance(new BigDecimal("5200.00"));

        MimeMessage message = mimeMessage(body);
        messageParser.transaction = transaction;
        statementService.statement = statement;
        spendingPaceService.expectedSpendingToday = new BigDecimal("4666.67");
        transactionInsightProperties.setEnabled(true);

        messageService.process(message);

        MessageRequest insightMessage = notificationService.messages.get(1);
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
    void shouldNotSendInsightMessageWhenDisabled() throws Exception {
        String body = "transaction email body";
        Transaction transaction = transaction();
        Statement statement = new Statement();
        statement.setStartDate(LocalDate.of(2026, 5, 16));
        statement.setClosingDate(LocalDate.of(2026, 6, 14));
        statement.setTargetStatementBalance(new BigDecimal("14000.00"));
        statement.setCurrentBalance(new BigDecimal("5200.00"));

        MimeMessage message = mimeMessage(body);
        messageParser.transaction = transaction;
        statementService.statement = statement;
        spendingPaceService.expectedSpendingToday = new BigDecimal("4666.67");

        messageService.process(message);

        Assertions.assertEquals(1, notificationService.messages.size());
        Assertions.assertEquals("HX_TRANSACTION_TEMPLATE", notificationService.messages.getFirst().contentSid());
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

    private MimeMessage mimeMessage(String body) throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setSubject("notification");
        message.setContent(mimeMultipart(body));
        message.saveChanges();
        return message;
    }

    private MimeMultipart mimeMultipart(String body) throws Exception {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(body);

        MimeMultipart mimeMultipart = new MimeMultipart();
        mimeMultipart.addBodyPart(bodyPart);
        return mimeMultipart;
    }

    private TransactionRepository transactionRepository() {
        return (TransactionRepository) Proxy.newProxyInstance(
                TransactionRepository.class.getClassLoader(),
                new Class<?>[] { TransactionRepository.class },
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        savedTransaction = (Transaction) args[0];
                        savedTransaction.setId(123L);
                        return savedTransaction;
                    }
                    if ("toString".equals(method.getName())) {
                        return "StubTransactionRepository";
                    }
                    throw new UnsupportedOperationException("Unsupported repository method: " + method.getName());
                });
    }

    private static class StubMessageParser extends MessageParser {

        private String body;
        private Transaction transaction;

        @Override
        public Transaction parseTransactions(String body) {
            this.body = body;
            return transaction;
        }
    }

    private static class StubStatementService extends StatementService {

        private LocalDate transactionDate;
        private Transaction balanceTransaction;
        private Statement statement;
        private Statement incrementedStatement;

        private StubStatementService() {
            super(null, new FinanceStatementProperties());
        }

        @Override
        public Statement findOrCreateStatementFor(LocalDate transactionDate) {
            this.transactionDate = transactionDate;
            return statement;
        }

        @Override
        public Statement incrementCurrentBalance(Transaction transaction) {
            this.balanceTransaction = transaction;
            return incrementedStatement == null ? transaction.getStatement() : incrementedStatement;
        }
    }

    private static class StubSpendingPaceService extends SpendingPaceService {

        private Transaction transaction;
        private Statement statement;
        private BigDecimal expectedSpendingToday = BigDecimal.ZERO;

        @Override
        public BigDecimal evaluate(Transaction transaction, Statement statement) {
            this.transaction = transaction;
            this.statement = statement;
            return expectedSpendingToday;
        }
    }

    private static class StubWhatsAppNotificationService extends WhatsAppNotificationService {

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
