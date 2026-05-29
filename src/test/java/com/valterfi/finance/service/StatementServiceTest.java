package com.valterfi.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.valterfi.finance.config.FinanceStatementProperties;
import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.repository.StatementRepository;

class StatementServiceTest {

    private StatementRepository statementRepository;
    private FinanceStatementProperties properties;
    private Optional<Statement> existingStatement;
    private Optional<Statement> latestStatement;
    private List<Statement> savedStatements;
    private AtomicLong ids;

    private StatementService statementService;

    @BeforeEach
    void setUp() {
        existingStatement = Optional.empty();
        latestStatement = Optional.empty();
        savedStatements = new ArrayList<>();
        ids = new AtomicLong(1L);
        properties = new FinanceStatementProperties();
        statementRepository = statementRepository();
        statementService = new StatementService(statementRepository, properties);
    }

    @Test
    void shouldReturnExistingStatementWhenTransactionDateIsInsideStatementRange() {
        LocalDate transactionDate = LocalDate.of(2026, 5, 25);
        Statement existingStatement = statement(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 29));

        this.existingStatement = Optional.of(existingStatement);

        Statement result = statementService.findOrCreateStatementFor(transactionDate);

        assertSame(existingStatement, result);
        assertEquals(0, savedStatements.size());
    }

    @Test
    void shouldCreateFirstStatementWhenNoStatementExists() {
        LocalDate transactionDate = LocalDate.of(2026, 5, 25);
        properties.setTargetStatementBalance(new BigDecimal("15000.00"));

        Statement result = statementService.findOrCreateStatementFor(transactionDate);

        assertEquals(1L, result.getId());
        assertEquals(LocalDate.of(2026, 5, 1), result.getReferenceMonth());
        assertEquals(LocalDate.of(2026, 5, 1), result.getStartDate());
        assertEquals(LocalDate.of(2026, 5, 29), result.getClosingDate());
        assertEquals(BigDecimal.ZERO, result.getCurrentBalance());
        assertEquals(new BigDecimal("15000.00"), result.getTargetStatementBalance());
    }

    @Test
    void shouldCreateMissingStatementsUntilTransactionDateIsCovered() {
        LocalDate transactionDate = LocalDate.of(2026, 6, 10);
        Statement aprilStatement = statement(
                10L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 4, 24));
        ids = new AtomicLong(11L);
        latestStatement = Optional.of(aprilStatement);

        Statement result = statementService.findOrCreateStatementFor(transactionDate);

        assertEquals(2, savedStatements.size());
        Statement mayStatement = savedStatements.getFirst();
        assertEquals(LocalDate.of(2026, 5, 1), mayStatement.getReferenceMonth());
        assertEquals(LocalDate.of(2026, 4, 25), mayStatement.getStartDate());
        assertEquals(LocalDate.of(2026, 5, 29), mayStatement.getClosingDate());

        assertEquals(LocalDate.of(2026, 6, 1), result.getReferenceMonth());
        assertEquals(LocalDate.of(2026, 5, 30), result.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 26), result.getClosingDate());
    }

    @Test
    void shouldCreateNextStatementWhenCandidateClosingDateEqualsTransactionDate() {
        LocalDate transactionDate = LocalDate.of(2026, 5, 29);
        Statement aprilStatement = statement(
                10L,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 4, 24));
        ids = new AtomicLong(11L);
        latestStatement = Optional.of(aprilStatement);

        Statement result = statementService.findOrCreateStatementFor(transactionDate);

        assertEquals(2, savedStatements.size());
        Statement mayStatement = savedStatements.getFirst();
        assertEquals(LocalDate.of(2026, 5, 1), mayStatement.getReferenceMonth());
        assertEquals(LocalDate.of(2026, 4, 25), mayStatement.getStartDate());
        assertEquals(LocalDate.of(2026, 5, 29), mayStatement.getClosingDate());

        assertEquals(LocalDate.of(2026, 6, 1), result.getReferenceMonth());
        assertEquals(LocalDate.of(2026, 5, 30), result.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 26), result.getClosingDate());
    }

    @Test
    void shouldIncrementStatementCurrentBalanceWithTransactionAmount() {
        Statement statement = statement(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 29));
        statement.setCurrentBalance(new BigDecimal("100.00"));

        Transaction transaction = new Transaction();
        transaction.setId(10L);
        transaction.setAmount(new BigDecimal("202.44"));
        transaction.setStatement(statement);

        Statement result = statementService.incrementCurrentBalance(transaction);

        assertSame(statement, result);
        assertEquals(new BigDecimal("302.44"), result.getCurrentBalance());
        assertNotNull(result.getUpdatedAt());
        assertEquals(1, savedStatements.size());
    }

    @Test
    void shouldIncrementStatementCurrentBalanceWhenCurrentBalanceIsNull() {
        Statement statement = statement(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 29));

        Transaction transaction = new Transaction();
        transaction.setId(10L);
        transaction.setAmount(new BigDecimal("202.44"));
        transaction.setStatement(statement);

        Statement result = statementService.incrementCurrentBalance(transaction);

        assertEquals(new BigDecimal("202.44"), result.getCurrentBalance());
        assertNotNull(result.getUpdatedAt());
    }

    private Statement statement(Long id, LocalDate referenceMonth, LocalDate startDate, LocalDate closingDate) {
        Statement statement = new Statement();
        statement.setId(id);
        statement.setReferenceMonth(referenceMonth);
        statement.setStartDate(startDate);
        statement.setClosingDate(closingDate);
        return statement;
    }

    private StatementRepository statementRepository() {
        return (StatementRepository) Proxy.newProxyInstance(
                StatementRepository.class.getClassLoader(),
                new Class<?>[] { StatementRepository.class },
                (proxy, method, args) -> {
                    if ("findFirstByStartDateLessThanEqualAndClosingDateGreaterThanAndDeletedFalse".equals(method.getName())) {
                        return existingStatement;
                    }
                    if ("findFirstByDeletedFalseOrderByReferenceMonthDesc".equals(method.getName())) {
                        return latestStatement;
                    }
                    if ("save".equals(method.getName())) {
                        Statement statement = (Statement) args[0];
                        statement.setId(ids.getAndIncrement());
                        savedStatements.add(statement);
                        return statement;
                    }
                    if ("toString".equals(method.getName())) {
                        return "StubStatementRepository";
                    }
                    throw new UnsupportedOperationException("Unsupported repository method: " + method.getName());
                });
    }
}
