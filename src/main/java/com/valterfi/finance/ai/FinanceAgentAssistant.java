package com.valterfi.finance.ai;

import dev.langchain4j.service.spring.AiService;

@AiService
public interface FinanceAgentAssistant {

    String chat(String userMessage);

}
