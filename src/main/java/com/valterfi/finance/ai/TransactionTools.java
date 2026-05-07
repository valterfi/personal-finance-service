package com.valterfi.finance.ai;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.service.TransactionService;

import dev.langchain4j.agent.tool.Tool;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component("transactionTools")
public class TransactionTools {

    private final TransactionService transactionService;

    @Tool("Use esta ferramenta quando o usuario pedir as transacoes de hoje.")
    public String findTodayTransactions() {
        return formatTransactions(transactionService.findTodayTransactions());
    }

    @Tool("Use esta ferramenta quando o usuario pedir transacoes de uma data especifica.")
    public String findTransactionsByDate(String date) {
        return formatTransactions(transactionService.findTransactionsByDate(LocalDate.parse(date)));
    }

    private String formatTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "Nenhuma transacao encontrada.";
        }

        return transactions.toString();
    }

}
