package com.valterfi.finance.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "finance.statement")
public class FinanceStatementProperties {

    private BigDecimal targetStatementBalance = new BigDecimal("14000.00");

}
