package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Page<Card> findByOwner_Id(Long ownerId, Pageable pageable);

    Optional<Card> findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);

    Optional<Card> findByIdAndOwner_Id(Long id, Long ownerId);
}