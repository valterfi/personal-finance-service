package com.valterfi.finance.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "twilio.whatsapp")
public class TwilioWhatsAppProperties {

    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String transactionTemplateId;
    private List<String> toNumbers = new ArrayList<>();

}
