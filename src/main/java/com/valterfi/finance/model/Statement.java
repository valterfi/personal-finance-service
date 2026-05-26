package com.valterfi.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.valterfi.finance.util.BrazilDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "statement")
@Getter
@Setter
@ToString
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "current_balance", precision = 19, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "statement_balance", precision = 19, scale = 2)
    private BigDecimal statementBalance;

    @Column(name = "target_statement_balance", precision = 19, scale = 2)
    private BigDecimal targetStatementBalance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "replaced_for")
    private Long replacedFor;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    void onCreate() {
        createdAt = BrazilDateTime.now();
        deleted = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = BrazilDateTime.now();
    }
}
