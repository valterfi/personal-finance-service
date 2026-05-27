package com.valterfi.finance.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.repository.TransactionRepository;
import com.valterfi.finance.util.MessageUtils;

import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageParser messageParser;
    private final TransactionRepository transactionRepository;
    private final StatementService statementService;
    private final SpendingPaceService spendingPaceService;
    private final TransactionNotificationService transactionNotificationService;

    public void process(Message message) {
        try {
            String body = MessageUtils.extractBody(message);
            Transaction transaction = messageParser.parseTransactions(body);

            if (transaction == null) {
                log.info("No transaction pattern matched for message subject={}", message.getSubject());
                return;
            }

            log.info("Parsed transaction: {}", transaction);

            transaction.setStatement(statementService.findOrCreateStatementFor(transaction.getDate()));
            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Persisted transaction id={}", savedTransaction.getId());
            Statement updatedStatement = statementService.incrementCurrentBalance(savedTransaction);
            BigDecimal expectedSpendingToday = spendingPaceService.evaluate(savedTransaction, updatedStatement);

            transactionNotificationService.sendInsightMessage(savedTransaction, expectedSpendingToday);
        } catch (Exception exception) {
            log.error("Failed to process message subject={}", MessageUtils.safeSubject(message), exception);
        }
    }

}
