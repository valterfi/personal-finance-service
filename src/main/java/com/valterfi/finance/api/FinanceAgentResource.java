package com.valterfi.finance.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.valterfi.finance.ai.FinanceAgentAssistant;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/finance")
@AllArgsConstructor
public class FinanceAgentResource {

    private final FinanceAgentAssistant assistant;

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String ask(@RequestBody String question) {
        return assistant.chat(question);
    }

}
