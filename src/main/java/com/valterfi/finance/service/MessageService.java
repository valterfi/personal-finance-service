package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import lombok.extern.slf4j.Slf4j;

import com.valterfi.finance.model.Transaction;

@Slf4j
@Service
public class MessageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "(?<notificationDate>\\d{2}/\\d{2}/\\d{4}) Compra no crédito aprovada Sua compra no cartão final (?<card>\\d{4}) "
                    + "no valor de R\\$ (?<amount>\\d+[\\.,]\\d{2}), dia (?<purchaseDate>\\d{2}/\\d{2}/\\d{4}) às (?<time>\\d{2}:\\d{2}), "
                    + "em (?<description>.*?), foi aprovada\\.?");

    public void process(Message message) {
        try {
            String body = extractBody(message);
            List<Transaction> transactions = parseTransactions(body);

            for (Transaction transaction : transactions) {
                log.info("Parsed transaction: {}", transaction);
            }

            if (transactions.isEmpty()) {
                log.info("No transaction pattern matched for message subject={}", message.getSubject());
            }
        } catch (Exception exception) {
            log.error("Failed to process message subject={}", safeSubject(message), exception);
        }
    }

    List<Transaction> parseTransactions(String body) {
        Matcher matcher = TRANSACTION_PATTERN.matcher(body);
        List<Transaction> transactions = new ArrayList<>();

        while (matcher.find()) {
            Transaction transaction = new Transaction();
            transaction.setDate(LocalDate.parse(matcher.group("purchaseDate"), DATE_FORMATTER));
            transaction.setTime(LocalTime.parse(matcher.group("time"), TIME_FORMATTER));
            transaction.setDescription(matcher.group("description").trim());
            transaction.setAmount(parseAmount(matcher.group("amount")));
            transaction.setCard(matcher.group("card"));
            transactions.add(transaction);
        }

        return transactions;
    }

    private String extractBody(Message message) throws Exception {
        Object content = message.getContent();
        return content == null ? "" : String.valueOf(content);
    }

    private BigDecimal parseAmount(String amount) {
        return new BigDecimal(amount.replace(".", "").replace(",", "."));
    }

    private String safeSubject(Message message) {
        try {
            return message.getSubject();
        } catch (Exception exception) {
            return "[unavailable]";
        }
    }

}
