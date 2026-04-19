package com.valterfi.finance.mail;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mail.listener")
public class EmailListenerProperties {

    private boolean enabled;
    private String host;
    private int port = 993;
    private String username;
    private String password;
    private String protocol = "imaps";
    private String folder = "INBOX";
    private long pollIntervalMs = 60000;
    private boolean sslEnable = true;
    private boolean starttlsEnable;
    private boolean markAsRead = true;
}
