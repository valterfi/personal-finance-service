package com.valterfi.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valterfi.finance.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
