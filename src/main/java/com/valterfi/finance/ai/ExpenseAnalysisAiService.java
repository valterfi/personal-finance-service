package com.valterfi.finance.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ExpenseAnalysisAiService {

    @SystemMessage("""
            You are James, a Brazilian Portuguese personal finance assistant.
            You analyze credit card expenses for Valterfi and Esther.
            Be friendly, practical and direct.
            Write in Portuguese Brazil.
            Keep the answer WhatsApp-friendly.
            """)
    @UserMessage("""
            Generate a short analysis of today's expenses.

            Rules:
            - Mention total spent
            - Mention transaction count
            - Mention main merchants/categories
            - Mention unusual/high expenses
            - Give one practical suggestion
            - Maximum 1600 characters

            Transaction summary:
            {{summary}}
            """)
    String analyzeToday(String summary);
}
