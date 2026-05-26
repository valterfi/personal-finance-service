package com.valterfi.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.valterfi.finance.model.Statement;
import com.valterfi.finance.model.Transaction;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SpendingPaceService {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    public BigDecimal evaluate(Transaction transaction, Statement statement) {
        long totalCycleDays = ChronoUnit.DAYS.between(statement.getStartDate(), statement.getClosingDate()) + 1;
        long currentCycleDay = ChronoUnit.DAYS.between(statement.getStartDate(), transaction.getDate()) + 1;

        BigDecimal expectedSpendingToday = statement.getTargetStatementBalance()
                .multiply(BigDecimal.valueOf(currentCycleDay))
                .divide(BigDecimal.valueOf(totalCycleDays), 2, RoundingMode.HALF_UP);
        BigDecimal currentAccumulatedSpending = statement.getCurrentBalance();
        BigDecimal difference = currentAccumulatedSpending.subtract(expectedSpendingToday);
        String status = resolveSpendingPaceStatus(difference);

        log.info("Spending pace: status={}, current accumulated spending={}, expected spending until today={}, difference={}",
                status,
                formatAmount(currentAccumulatedSpending),
                formatAmount(expectedSpendingToday),
                formatAmount(difference));
        return expectedSpendingToday;
    }

    private String resolveSpendingPaceStatus(BigDecimal difference) {
        if (difference.signum() > 0) {
            return "TOO_FAST";
        }
        if (difference.signum() < 0) {
            return "TOO_SLOW";
        }
        return "ON_TARGET";
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(BRAZIL).format(amount);
    }
}
