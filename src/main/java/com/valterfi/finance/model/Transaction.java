package com.valterfi.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transaction {
    private LocalDate date;
    private LocalDateTime time;
    private String description;
    private BigDecimal amount;
    private String card;
}
