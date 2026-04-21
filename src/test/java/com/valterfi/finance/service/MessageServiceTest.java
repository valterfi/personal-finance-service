package com.valterfi.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.valterfi.finance.model.Transaction;

class MessageServiceTest {

    private static final String SAMPLE_BODY = """
            16/04/2026 Compra no crédito aprovada Sua compra no cartão final 7396 no valor de R$ 32,98, dia 16/04/2026 às 08:16, em DL*UberRides Sao Paulo BRA, foi aprovada.
            16/04/2026 Compra no crédito aprovada Sua compra no cartão final 7396 no valor de R$ 32,99, dia 16/04/2026 às 08:34, em DL*UberRides Sao Paulo BRA, foi aprovada
            16/04/2026 Compra no crédito aprovada Sua compra no cartão final 5149 no valor de R$ 40,36, dia 16/04/2026 às 11:48, em IFD*EQUILIBRIO VIDA GO SAO PAULO BRA, foi aprovada.
            16/04/2026 Compra no crédito aprovada Sua compra no cartão final 5149 no valor de R$ 22,00, dia 16/04/2026 às 21:33, em JIM.COM* SP PARK METRO SAO PAULO BRA, foi aprovada.
            17/04/2026 Compra no crédito aprovada Sua compra no cartão final 5149 no valor de R$ 12,90, dia 17/04/2026 às 07:25, em IFD*IFOOD CLUB Osasco BRA, foi aprovada.
            18/04/2026 Compra no crédito aprovada Sua compra no cartão final 5149 no valor de R$ 29,90, dia 18/04/2026 às 13:41, em BRIGADERIA SAO PAULO BRA, foi aprovada.
            18/04/2026 Compra no crédito aprovada Sua compra no cartão final 7396 no valor de R$ 202,44, dia 18/04/2026 às 13:59, em CARREFOUR TTE 43 SAO PAULO BRA, foi aprovada.
            """;

    private final MessageService messageService = new MessageService();

    @Test
    void shouldParseAllTransactionsFromSampleBody() {
        List<Transaction> transactions = messageService.parseTransactions(SAMPLE_BODY);

        assertEquals(7, transactions.size());
    }

    @Test
    void shouldParseLastTransactionFieldsFromSampleBody() {
        List<Transaction> transactions = messageService.parseTransactions(SAMPLE_BODY);
        Transaction transaction = transactions.getLast();

        assertEquals(LocalDate.of(2026, 4, 18), transaction.getDate());
        assertEquals(LocalTime.of(13, 59), transaction.getTime());
        assertEquals("CARREFOUR TTE 43 SAO PAULO BRA", transaction.getDescription());
        assertEquals(new BigDecimal("202.44"), transaction.getAmount());
        assertEquals("7396", transaction.getCard());
    }

    @Test
    void shouldReturnEmptyListWhenBodyDoesNotMatchPattern() {
        List<Transaction> transactions = messageService.parseTransactions("random content");

        assertTrue(transactions.isEmpty());
    }
}
