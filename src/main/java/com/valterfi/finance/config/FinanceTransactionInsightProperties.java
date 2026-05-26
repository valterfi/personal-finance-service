package com.valterfi.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "finance.transaction-insight")
public class FinanceTransactionInsightProperties {

    private boolean enabled;

}
