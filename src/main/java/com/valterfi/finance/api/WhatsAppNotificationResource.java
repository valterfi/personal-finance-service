package com.valterfi.finance.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.valterfi.finance.service.WhatsAppNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppNotificationResource {

    private final WhatsAppNotificationService notificationService;

    @PostMapping(value = "/test", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String sendTestMessage(@RequestBody String text) {
        notificationService.sendMessage(text);
        return "WhatsApp message sent.";
    }

}
