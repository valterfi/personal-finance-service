package com.valterfi.finance.service;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private StubTransactionWhatsAppNotificationService transactionWhatsAppNotificationService;
    private TransactionRepository transactionRepository;
    private Transaction savedTransaction;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageParser = new StubMessageParser();
        statementService = new StubStatementService();
        spendingPaceService = new StubSpendingPaceService();
        transactionWhatsAppNotificationService = new StubTransactionWhatsAppNotificationService();
        transactionRepository = transactionRepository();

        messageService = new MessageService(
                messageParser,
                transactionRepository,
                statementService,
                spendingPaceService,
                transactionWhatsAppNotificationService);
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
        Assertions.assertSame(transaction, transactionWhatsAppNotificationService.transactionMessage);
        Assertions.assertEquals(LocalDate.of(2026, 5, 25), statementService.transactionDate);
        Assertions.assertSame(statement, savedTransaction.getStatement());
        Assertions.assertEquals(123L, savedTransaction.getId());
        Assertions.assertSame(savedTransaction, statementService.balanceTransaction);
        Assertions.assertSame(savedTransaction, spendingPaceService.transaction);
        Assertions.assertSame(statement, spendingPaceService.statement);
        Assertions.assertSame(savedTransaction, transactionWhatsAppNotificationService.insightTransaction);
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
        Assertions.assertNull(transactionWhatsAppNotificationService.transactionMessage);
        Assertions.assertNull(transactionWhatsAppNotificationService.insightTransaction);
        Assertions.assertNull(savedTransaction);
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

    private static class StubTransactionWhatsAppNotificationService extends TransactionNotificationService {

        private Transaction transactionMessage;
        private Transaction insightTransaction;
        private BigDecimal expectedSpendingToday;

        private StubTransactionWhatsAppNotificationService() {
            super(
                    new TwilioService(new TwilioWhatsAppProperties()),
                    new TwilioWhatsAppProperties(),
                    new FinanceTransactionInsightProperties(),
                    new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public void sendMessage(Transaction transaction) {
            this.transactionMessage = transaction;
        }

        @Override
        public void sendInsightMessage(Transaction transaction, BigDecimal expectedSpendingToday) {
            this.insightTransaction = transaction;
            this.expectedSpendingToday = expectedSpendingToday;
        }
    }
}
