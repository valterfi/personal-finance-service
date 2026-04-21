package com.valterfi.finance.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.util.MessageUtils;

@Component
public class MessageParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "(?<notificationDate>\\d{2}/\\d{2}/\\d{4})\\s+Compra\\s+no\\s+crédito\\s+aprovada\\s+Sua\\s+compra\\s+no\\s+cartão\\s+final\\s+(?<card>\\d{4})\\s+"
                    + "no\\s+valor\\s+de\\s+R\\$\\s+(?<amount>\\d+[\\.,]\\d{2}),\\s+dia\\s+(?<purchaseDate>\\d{2}/\\d{2}/\\d{4})\\s+às\\s+(?<time>\\d{2}:\\d{2}),\\s+"
                    + "em\\s+(?<description>.*?),\\s+foi\\s+aprovada\\.?");

    public List<Transaction> parseTransactions(String body) {
        Matcher matcher = TRANSACTION_PATTERN.matcher(body);
        List<Transaction> transactions = new ArrayList<>();

        while (matcher.find()) {
            Transaction transaction = new Transaction();
            transaction.setDate(LocalDate.parse(matcher.group("purchaseDate"), DATE_FORMATTER));
            transaction.setTime(LocalTime.parse(matcher.group("time"), TIME_FORMATTER));
            transaction.setDescription(MessageUtils.normalizeWhitespace(matcher.group("description")));
            transaction.setAmount(MessageUtils.parseAmount(matcher.group("amount")));
            transaction.setCard(matcher.group("card"));
            transactions.add(transaction);
        }

        return transactions;
    }
}
