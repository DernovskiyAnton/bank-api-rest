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

    @Query("SELECT t FROM Transaction t WHERE t.sourceCard.id = :cardId OR t.destinationCard.id = :cardId ORDER BY t.createdAt DESC")
    Page<Transaction> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceCard.owner.id = :userId OR t.destinationCard.owner.id = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);
}