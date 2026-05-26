package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.valterfi.finance.config.FinanceStatementProperties;
import com.valterfi.finance.model.Statement;
import com.valterfi.finance.repository.StatementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private static final BigDecimal DEFAULT_CURRENT_BALANCE = BigDecimal.ZERO;

    private final StatementRepository statementRepository;
    private final FinanceStatementProperties properties;

    public Statement findOrCreateStatementFor(LocalDate transactionDate) {
        log.debug("Finding statement for transactionDate={}", transactionDate);

        return statementRepository
                .findFirstByStartDateLessThanEqualAndClosingDateGreaterThanEqualAndDeletedFalse(
                        transactionDate,
                        transactionDate)
                .map(statement -> {
                    log.debug("Found statement id={} referenceMonth={} startDate={} closingDate={} for transactionDate={}",
                            statement.getId(),
                            statement.getReferenceMonth(),
                            statement.getStartDate(),
                            statement.getClosingDate(),
                            transactionDate);
                    return statement;
                })
                .orElseGet(() -> {
                    log.debug("No statement found for transactionDate={}. Creating statement range.", transactionDate);
                    return createStatementFor(transactionDate);
                });
    }

    private Statement createStatementFor(LocalDate transactionDate) {
        Statement lastStatement = statementRepository.findFirstByDeletedFalseOrderByReferenceMonthDesc()
                .orElse(null);
        log.debug("Latest statement before creation: {}", lastStatement);

        Statement statement = createNextStatement(lastStatement, transactionDate);
        while (statement.getClosingDate().isBefore(transactionDate)) {
            log.debug("Candidate statement referenceMonth={} startDate={} closingDate={} does not include transactionDate={}. Saving as missing statement.",
                    statement.getReferenceMonth(),
                    statement.getStartDate(),
                    statement.getClosingDate(),
                    transactionDate);
            lastStatement = statementRepository.save(statement);
            log.info("Created missing statement id={} referenceMonth={} startDate={} closingDate={}",
                    lastStatement.getId(),
                    lastStatement.getReferenceMonth(),
                    lastStatement.getStartDate(),
                    lastStatement.getClosingDate());
            statement = createNextStatement(lastStatement, transactionDate);
        }

        log.debug("Candidate statement referenceMonth={} startDate={} closingDate={} includes transactionDate={}. Saving target statement.",
                statement.getReferenceMonth(),
                statement.getStartDate(),
                statement.getClosingDate(),
                transactionDate);
        Statement savedStatement = statementRepository.save(statement);
        log.info("Created statement id={} referenceMonth={} startDate={} closingDate={}",
                savedStatement.getId(),
                savedStatement.getReferenceMonth(),
                savedStatement.getStartDate(),
                savedStatement.getClosingDate());
        return savedStatement;
    }

    private Statement createNextStatement(Statement lastStatement, LocalDate transactionDate) {
        LocalDate referenceMonth = resolveReferenceMonth(lastStatement, transactionDate);

        Statement statement = new Statement();
        statement.setReferenceMonth(referenceMonth);
        statement.setStartDate(resolveStartDate(lastStatement, referenceMonth));
        statement.setClosingDate(lastFridayOfMonth(referenceMonth));
        statement.setCurrentBalance(DEFAULT_CURRENT_BALANCE);
        statement.setTargetStatementBalance(properties.getTargetStatementBalance());
        log.debug("Created statement candidate from lastStatementId={} transactionDate={} referenceMonth={} startDate={} closingDate={} currentBalance={} targetStatementBalance={}",
                lastStatement == null ? null : lastStatement.getId(),
                transactionDate,
                statement.getReferenceMonth(),
                statement.getStartDate(),
                statement.getClosingDate(),
                statement.getCurrentBalance(),
                statement.getTargetStatementBalance());
        return statement;
    }

    private LocalDate resolveReferenceMonth(Statement lastStatement, LocalDate transactionDate) {
        if (lastStatement == null) {
            LocalDate referenceMonth = transactionDate.withDayOfMonth(1);
            log.debug("Resolved referenceMonth={} from transactionDate={} because no previous statement exists",
                    referenceMonth,
                    transactionDate);
            return referenceMonth;
        }

        LocalDate referenceMonth = lastStatement.getReferenceMonth().plusMonths(1).withDayOfMonth(1);
        log.debug("Resolved next referenceMonth={} from lastStatementId={} lastReferenceMonth={}",
                referenceMonth,
                lastStatement.getId(),
                lastStatement.getReferenceMonth());
        return referenceMonth;
    }

    private LocalDate resolveStartDate(Statement lastStatement, LocalDate referenceMonth) {
        if (lastStatement == null || lastStatement.getClosingDate() == null) {
            log.debug("Resolved startDate={} from referenceMonth={} because no previous closingDate exists",
                    referenceMonth,
                    referenceMonth);
            return referenceMonth;
        }

        LocalDate startDate = lastStatement.getClosingDate().plusDays(1);
        log.debug("Resolved startDate={} from lastStatementId={} lastClosingDate={}",
                startDate,
                lastStatement.getId(),
                lastStatement.getClosingDate());
        return startDate;
    }

    private LocalDate lastFridayOfMonth(LocalDate referenceMonth) {
        LocalDate date = YearMonth.from(referenceMonth).atEndOfMonth();
        while (date.getDayOfWeek() != DayOfWeek.FRIDAY) {
            date = date.minusDays(1);
        }
        log.debug("Resolved last Friday of referenceMonth={} as closingDate={}", referenceMonth, date);
        return date;
    }
}
