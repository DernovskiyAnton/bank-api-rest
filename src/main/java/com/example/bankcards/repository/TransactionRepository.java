package com.example.bankcards.repository;

import com.example.bankcards.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId ORDER BY t.timestamp DESC")
    Page<Transaction> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromCard.user.id = :userId OR t.toCard.user.id = :userId ORDER BY t.timestamp DESC")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);
}