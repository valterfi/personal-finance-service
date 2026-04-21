package com.valterfi.finance.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "mail.listener")
public class EmailListenerProperties {

    private boolean enabled;
    private String host;

    @Min(1)
    private int port = 993;

    private String username;
    private String password;
    private String protocol = "imap";
    private String folder = "INBOX";
    private String subjectText = "";

    @NotBlank
    private String cronExpression = "0 */1 * * * *";

    private boolean sslEnable = true;
    private boolean starttlsEnable;
    private boolean markAsRead = true;

}
