package com.valterfi.finance.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valterfi.finance.model.Statement;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    Optional<Statement> findFirstByStartDateLessThanEqualAndClosingDateGreaterThanEqualAndDeletedFalse(
            LocalDate startDate,
            LocalDate closingDate);

    Optional<Statement> findFirstByDeletedFalseOrderByReferenceMonthDesc();
}
