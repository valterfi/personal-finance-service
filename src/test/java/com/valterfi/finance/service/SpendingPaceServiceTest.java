package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;

class SpendingPaceServiceTest {

    private final SpendingPaceService spendingPaceService = new SpendingPaceService();

    @Test
    void shouldReturnExpectedSpendingTodayWhenSpendingTooFast() {
        Statement statement = statement(new BigDecimal("5200.00"));
        Transaction transaction = transaction(LocalDate.of(2026, 5, 25));

        BigDecimal expectedSpendingToday = spendingPaceService.evaluate(transaction, statement);

        Assertions.assertEquals(new BigDecimal("4666.67"), expectedSpendingToday);
    }

    @Test
    void shouldReturnExpectedSpendingTodayWhenSpendingTooSlow() {
        Statement statement = statement(new BigDecimal("202.44"));
        Transaction transaction = transaction(LocalDate.of(2026, 5, 25));

        BigDecimal expectedSpendingToday = spendingPaceService.evaluate(transaction, statement);

        Assertions.assertEquals(new BigDecimal("4666.67"), expectedSpendingToday);
    }

    @Test
    void shouldReturnExpectedSpendingTodayWhenSpendingIsOnTarget() {
        Statement statement = statement(new BigDecimal("4666.67"));
        Transaction transaction = transaction(LocalDate.of(2026, 5, 25));

        BigDecimal expectedSpendingToday = spendingPaceService.evaluate(transaction, statement);

        Assertions.assertEquals(new BigDecimal("4666.67"), expectedSpendingToday);
    }

    private Statement statement(BigDecimal currentBalance) {
        Statement statement = new Statement();
        statement.setStartDate(LocalDate.of(2026, 5, 16));
        statement.setClosingDate(LocalDate.of(2026, 6, 14));
        statement.setTargetStatementBalance(new BigDecimal("14000.00"));
        statement.setCurrentBalance(currentBalance);
        return statement;
    }

    private Transaction transaction(LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setDate(date);
        return transaction;
    }
}
