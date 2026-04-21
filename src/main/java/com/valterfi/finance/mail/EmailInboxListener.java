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
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import com.valterfi.finance.service.MessageService;
import com.valterfi.finance.util.MessageUtils;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnProperty(prefix = "mail.listener", name = "enabled", havingValue = "true")
public class EmailInboxListener {

    private final EmailListenerProperties properties;
    private final MessageService messageService;

    @Scheduled(cron = "${mail.listener.cron-expression}")
    public void pollInbox() {
        Properties mailProperties = new Properties();
        String protocol = resolveStoreProtocol();

        log.info(
                "Polling inbox host={}, port={}, folder={}, protocol={}, subjectText={}, markAsRead={}",
                properties.getHost(),
                properties.getPort(),
                properties.getFolder(),
                protocol,
                properties.getSubjectText(),
                properties.isMarkAsRead());

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

                Message[] unreadMessages = inbox.search(buildSearchTerm());
                log.info("Found {} unread messages matching the configured filter", unreadMessages.length);

                for (Message message : unreadMessages) {
                    log.info("Unread email: subject={}, body={}", message.getSubject(), MessageUtils.extractBody(message));
                    messageService.process(message);
                    if (properties.isMarkAsRead()) {
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                }
            }
        } catch (Exception exception) {
            log.error("Failed to poll inbox host={}, folder={}", properties.getHost(), properties.getFolder(), exception);
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

    private SearchTerm buildSearchTerm() {
        SearchTerm unreadTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

        if (!StringUtils.hasText(properties.getSubjectText())) {
            return unreadTerm;
        }

        return new AndTerm(unreadTerm, new SubjectTerm(properties.getSubjectText()));
    }
}
