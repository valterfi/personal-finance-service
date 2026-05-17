package com.valterfi.finance.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.valterfi.finance.service.ExpenseAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expense-analysis")
@RequiredArgsConstructor
public class ExpenseAnalysisResource {

    private final ExpenseAnalysisService expenseAnalysisService;

    @PostMapping(value = "/test", produces = MediaType.TEXT_PLAIN_VALUE)
    public String sendDailyAnalysis() {
        return expenseAnalysisService.analyzeTodayExpenses();
    }

}
