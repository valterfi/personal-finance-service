package com.valterfi.finance.service;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.util.MessageUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageParser messageParser;

    public void process(Message message) {
        try {
            String body = MessageUtils.extractBody(message);
            List<Transaction> transactions = messageParser.parseTransactions(body);

            for (Transaction transaction : transactions) {
                log.info("Parsed transaction: {}", transaction);
            }

            if (transactions.isEmpty()) {
                log.info("No transaction pattern matched for message subject={}", message.getSubject());
            }
        } catch (Exception exception) {
            log.error("Failed to process message subject={}", MessageUtils.safeSubject(message), exception);
        }
    }

}
