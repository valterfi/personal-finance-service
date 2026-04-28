package com.valterfi.finance.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.valterfi.finance.model.Transaction;

@SpringBootTest
@Transactional
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldPersistTransaction() {
        Transaction transaction = new Transaction();
        transaction.setDate(LocalDate.of(2026, 4, 18));
        transaction.setTime(LocalTime.of(13, 59));
        transaction.setDescription("CARREFOUR TTE 43 SAO PAULO BRA");
        transaction.setAmount(new BigDecimal("202.44"));
        transaction.setCard("7396");
        transaction.setInstallment(1);
        transaction.setTotalInstallments(1);
        transaction.setMemo("Groceries");
        transaction.setUpdateMemo("Imported from email");

        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

        Transaction persistedTransaction = transactionRepository.findById(savedTransaction.getId()).orElseThrow();

        assertNotNull(persistedTransaction.getId());
        assertEquals(LocalDate.of(2026, 4, 18), persistedTransaction.getDate());
        assertEquals(LocalTime.of(13, 59), persistedTransaction.getTime());
        assertEquals("CARREFOUR TTE 43 SAO PAULO BRA", persistedTransaction.getDescription());
        assertEquals(new BigDecimal("202.44"), persistedTransaction.getAmount());
        assertEquals("7396", persistedTransaction.getCard());
        assertEquals(1, persistedTransaction.getInstallment());
        assertEquals(1, persistedTransaction.getTotalInstallments());
        assertEquals("Groceries", persistedTransaction.getMemo());
        assertEquals("Imported from email", persistedTransaction.getUpdateMemo());
        assertNotNull(persistedTransaction.getCreatedAt());
        assertFalse(persistedTransaction.isDeleted());
    }
}
