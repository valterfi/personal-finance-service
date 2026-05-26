package com.valterfi.finance.service;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valterfi.finance.config.FinanceStatementProperties;
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
    private StubWhatsAppNotificationService notificationService;
    private TransactionRepository transactionRepository;
    private Transaction savedTransaction;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageParser = new StubMessageParser();
        statementService = new StubStatementService();
        notificationService = new StubWhatsAppNotificationService();
        transactionRepository = transactionRepository();

        TwilioWhatsAppProperties whatsAppProperties = new TwilioWhatsAppProperties();
        whatsAppProperties.setTransactionTemplateId("HX_TRANSACTION_TEMPLATE");
        messageService = new MessageService(
                messageParser,
                transactionRepository,
                statementService,
                notificationService,
                whatsAppProperties,
                new ObjectMapper());
    }

    @Test
    void shouldAttachStatementAndPersistParsedTransaction() throws Exception {
        String body = "transaction email body";
        Transaction transaction = transaction();
        Statement statement = new Statement();
        statement.setId(99L);

        MimeMessage message = mimeMessage(body);
        messageParser.transaction = transaction;
        statementService.statement = statement;

        messageService.process(message);

        Assertions.assertEquals(body, messageParser.body);
        Assertions.assertEquals("HX_TRANSACTION_TEMPLATE", notificationService.contentSid);
        Assertions.assertTrue(notificationService.contentVariables.contains("\"1\":\"7396\""));
        Assertions.assertEquals(LocalDate.of(2026, 5, 25), statementService.transactionDate);
        Assertions.assertSame(statement, savedTransaction.getStatement());
        Assertions.assertEquals(123L, savedTransaction.getId());
        Assertions.assertSame(savedTransaction, statementService.balanceTransaction);
    }

    @Test
    void shouldNotPersistWhenBodyDoesNotMatchTransactionPattern() throws Exception {
        String body = "random email body";

        MimeMessage message = mimeMessage(body);

        messageService.process(message);

        Assertions.assertEquals(body, messageParser.body);
        Assertions.assertNull(statementService.transactionDate);
        Assertions.assertNull(statementService.balanceTransaction);
        Assertions.assertNull(notificationService.contentSid);
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
            return transaction.getStatement();
        }
    }

    private static class StubWhatsAppNotificationService extends WhatsAppNotificationService {

        private String contentSid;
        private String contentVariables;

        private StubWhatsAppNotificationService() {
            super(new TwilioWhatsAppProperties());
        }

        @Override
        public void sendMessage(String contentSid, String contentVariables) {
            this.contentSid = contentSid;
            this.contentVariables = contentVariables;
        }
    }
}
