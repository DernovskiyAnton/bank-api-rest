package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferRequestDto;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransferResponse transfer(TransferRequestDto request, Long userId) {

        Card fromCard = cardRepository.findById(request.fromCardId())
                .orElseThrow(() -> new CardNotFoundException(request.fromCardId()));

        Card toCard = cardRepository.findById(request.toCardId())
                .orElseThrow(() -> new CardNotFoundException(request.toCardId()));

        validateTransfer(fromCard, toCard, request.amount(), userId);

        fromCard.withdraw(request.amount());
        toCard.deposit(request.amount());

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transaction transaction = Transaction.builder()
                .sourceCard(fromCard)
                .destinationCard(toCard)
                .amount(request.amount())
                .description(request.description())
                .status(TransactionStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return new TransferResponse(
                saved.getId(),
                fromCard.getId(),
                toCard.getId(),
                request.amount(),
                "SUCCESS",
                "Transfer completed successfully",
                LocalDateTime.now());
    }

    public Page<TransactionDto> getTransactionHistory(Long userId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByUserId(userId, pageable);
        return transactions.map(this::mapToDto);
    }

    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount, Long userId) {

        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same card");
        }

        if (!fromCard.getOwner().getId().equals(userId)) {
            throw new InvalidTransferException("Source card does not belong to user");
        }
        if (!toCard.getOwner().getId().equals(userId)) {
            throw new InvalidTransferException("Destination card does not belong to user");
        }

        if (!fromCard.isActiveAndValid()) {
            throw new CardBlockedException(fromCard.getId(), "Source card is not active");
        }
        if (!toCard.isActiveAndValid()) {
            throw new CardBlockedException(toCard.getId(), "Destination card is not active");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be positive");
        }
    }

    private TransactionDto mapToDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getSourceCard().getId(),
                transaction.getDestinationCard().getId(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
