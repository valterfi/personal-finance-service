package com.valterfi.finance.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(contentRetriever = "contentRetriever", tools = { "transactionTools" })
public interface FinanceAgentAssistant {

    @SystemMessage("""
            Voce e um assistente financeiro de Valterfi e Esther, um especialista em nossas despesas.
            Sua principal responsabilidade e responder as perguntas de forma amigavel e precisa,
            baseando-se exclusivamente nas informacoes contidas nos documentos que lhe foram fornecidos.
            Nunca invente informacoes ou use conhecimento externo.
            Se a resposta para uma pergunta nao estiver nos documentos, voce deve responder educadamente:
            'Desculpe, mas nao tenho informacoes sobre isso. Posso ajudar com mais alguma duvida sobre suas despesas?'
            """)
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}
