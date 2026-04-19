package com.valterfi.finance.mail;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "mail.listener", name = "enabled", havingValue = "true")
public class EmailInboxListener {

    private final EmailListenerProperties properties;

    public EmailInboxListener(EmailListenerProperties properties) {
        this.properties = properties;
    }

    @Scheduled(cron = "${mail.listener.cron-expression}")
    public void pollInbox() {
        Properties mailProperties = new Properties();
        String protocol = resolveStoreProtocol();

        mailProperties.put("mail.store.protocol", protocol);
        mailProperties.put("mail." + protocol + ".host", properties.getHost());
        mailProperties.put("mail." + protocol + ".port", String.valueOf(properties.getPort()));
        mailProperties.put("mail." + protocol + ".ssl.enable", String.valueOf(properties.isSslEnable()));
        mailProperties.put("mail." + protocol + ".starttls.enable", String.valueOf(properties.isStarttlsEnable()));

        Session session = Session.getInstance(mailProperties);

        try (Store store = session.getStore(protocol)) {
            store.connect(properties.getHost(), properties.getPort(), properties.getUsername(), properties.getPassword());

            try (Folder inbox = store.getFolder(properties.getFolder())) {
                inbox.open(Folder.READ_WRITE);

                Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                for (Message message : unreadMessages) {
                    System.out.println("Unread email: subject=" + message.getSubject() + ", body=" + extractBody(message));

                    if (properties.isMarkAsRead()) {
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to poll email inbox", exception);
        }
    }

    private String resolveStoreProtocol() {
        String configuredProtocol = properties.getProtocol();

        if (!properties.isSslEnable() && "imaps".equalsIgnoreCase(configuredProtocol)) {
            return "imap";
        }

        return configuredProtocol;
    }

    private String extractBody(Message message) {
        try {
            Object content = message.getContent();
            return normalizeBody(content == null ? "" : String.valueOf(content));
        } catch (Exception exception) {
            log.error("Failed to extract email body", exception);
            return "[unable to read body: " + exception.getMessage() + "]";
        }
    }

    private String normalizeBody(String body) {
        return body.replaceAll("\\s+", " ").trim();
    }
}
