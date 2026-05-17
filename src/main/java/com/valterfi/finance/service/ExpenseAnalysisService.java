package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.valterfi.finance.ai.ExpenseAnalysisAiService;
import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAnalysisService {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TransactionRepository transactionRepository;
    private final ExpenseAnalysisAiService expenseAnalysisAiService;

    public String analyzeTodayExpenses() {
        LocalDate currentDate = LocalDate.now();
        List<Transaction> transactions = transactionRepository.findByDateAndDeletedFalse(currentDate);
        String summary = buildCurrentDateSummary(currentDate, transactions);

        log.info("Generated current date transaction summary with {} transactions", transactions.size());
        return expenseAnalysisAiService.analyzeToday(summary);
    }

    private String buildCurrentDateSummary(LocalDate currentDate, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "Data de referencia: " + currentDate.format(DATE_FORMATTER) + "\nNenhuma transacao encontrada na data atual.";
        }

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String merchantTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getDescription,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(entry -> "- " + entry.getKey() + ": " + formatAmount(entry.getValue()))
                .collect(Collectors.joining("\n"));

        String transactionLines = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate).thenComparing(Transaction::getTime))
                .map(transaction -> "- %s %s | Cartao %s | %s | %s".formatted(
                        transaction.getDate().format(DATE_FORMATTER),
                        transaction.getTime().format(TIME_FORMATTER),
                        transaction.getCard(),
                        formatAmount(transaction.getAmount()),
                        transaction.getDescription()))
                .collect(Collectors.joining("\n"));

        return """
                Data de referencia: %s
                Total gasto: %s
                Quantidade de transacoes: %d

                Maiores comerciantes/descritivos:
                %s

                Transacoes:
                %s
                """.formatted(currentDate.format(DATE_FORMATTER), formatAmount(total), transactions.size(), merchantTotals, transactionLines);
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(amount);
    }

}
