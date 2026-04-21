package com.valterfi.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Transaction {
    private LocalDate date;
    private LocalTime time;
    private String description;
    private BigDecimal amount;
    private String card;
}
