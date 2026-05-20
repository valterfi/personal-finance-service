package com.valterfi.finance.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.valterfi.finance.model.Transaction;
import com.valterfi.finance.repository.TransactionRepository;
import com.valterfi.finance.util.BrazilDateTime;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> findTodayTransactions() {
        return findTransactionsByDate(BrazilDateTime.today());
    }

    public List<Transaction> findTransactionsByDate(LocalDate date) {
        return transactionRepository.findByDateAndDeletedFalse(date);
    }

}
