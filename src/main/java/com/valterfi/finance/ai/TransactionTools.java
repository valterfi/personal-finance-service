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

    @Tool("""
            Busca todas as transacoes de cartao registradas na data atual.
            Use quando o usuario perguntar sobre gastos, compras ou transacoes de hoje.
            Nao use esta ferramenta para datas passadas ou futuras.
            """)
    public String findTodayTransactions() {
        return formatTransactions(transactionService.findTodayTransactions());
    }

    @Tool("""
            Busca todas as transacoes de cartao registradas em uma data especifica.
            Use quando o usuario informar uma data concreta, como "2026-04-18" ou "18/04/2026".
            O parametro date deve ser enviado no formato ISO yyyy-MM-dd, por exemplo: 2026-04-18.
            """)
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
