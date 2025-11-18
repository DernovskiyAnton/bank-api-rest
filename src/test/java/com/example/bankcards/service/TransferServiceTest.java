package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferRequestDto;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardBlockedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_ValidRequest_ReturnsSuccessResponse() {
        Long userId = 1L;
        TransferRequestDto request = new TransferRequestDto(
                1L,
                2L,
                new BigDecimal("100.00"),
                "Test transfer"
        );

        User user = User.builder().id(userId).username("testuser").build();

        Card fromCard = Card.builder()
                .id(1L)
                .owner(user)
                .balance(new BigDecimal("500.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("1234")
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .owner(user)
                .balance(new BigDecimal("200.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("5678")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .sourceCard(fromCard)
                .destinationCard(toCard)
                .amount(request.amount())
                .description(request.description())
                .status(TransactionStatus.COMPLETED)
                .build();

        when(cardRepository.findById(request.fromCardId())).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(request.toCardId())).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenReturn(fromCard).thenReturn(toCard);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransferResponse result = transferService.transfer(request, userId);

        assertNotNull(result);
        assertEquals(1L, result.transactionId());
        assertEquals("SUCCESS", result.status());
        assertEquals(new BigDecimal("400.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("300.00"), toCard.getBalance());

        verify(cardRepository).findById(request.fromCardId());
        verify(cardRepository).findById(request.toCardId());
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transfer_InsufficientBalance_ThrowsException() {
        Long userId = 1L;
        TransferRequestDto request = new TransferRequestDto(
                1L,
                2L,
                new BigDecimal("600.00"),
                "Test transfer"
        );

        User user = User.builder().id(userId).username("testuser").build();

        Card fromCard = Card.builder()
                .id(1L)
                .owner(user)
                .balance(new BigDecimal("500.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("1234")
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .owner(user)
                .balance(new BigDecimal("200.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("5678")
                .build();

        when(cardRepository.findById(request.fromCardId())).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(request.toCardId())).thenReturn(Optional.of(toCard));

        assertThrows(Exception.class, () -> transferService.transfer(request, userId));

        verify(cardRepository).findById(request.fromCardId());
        verify(cardRepository).findById(request.toCardId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_SameCard_ThrowsException() {
        Long userId = 1L;
        TransferRequestDto request = new TransferRequestDto(
                1L,
                1L,
                new BigDecimal("100.00"),
                "Test transfer"
        );

        User user = User.builder().id(userId).username("testuser").build();

        Card card = Card.builder()
                .id(1L)
                .owner(user)
                .balance(new BigDecimal("500.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("1234")
                .build();

        when(cardRepository.findById(request.fromCardId())).thenReturn(Optional.of(card));
        when(cardRepository.findById(request.toCardId())).thenReturn(Optional.of(card));

        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transfer(request, userId)
        );

        assertEquals("Cannot transfer to the same card", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_BlockedSourceCard_ThrowsException() {
        Long userId = 1L;
        TransferRequestDto request = new TransferRequestDto(
                1L,
                2L,
                new BigDecimal("100.00"),
                "Test transfer"
        );

        User user = User.builder().id(userId).username("testuser").build();

        Card fromCard = Card.builder()
                .id(1L)
                .owner(user)
                .balance(new BigDecimal("500.00"))
                .status(CardStatus.BLOCKED)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("1234")
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .owner(user)
                .balance(new BigDecimal("200.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("5678")
                .build();

        when(cardRepository.findById(request.fromCardId())).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(request.toCardId())).thenReturn(Optional.of(toCard));

        CardBlockedException exception = assertThrows(
                CardBlockedException.class,
                () -> transferService.transfer(request, userId)
        );

        assertTrue(exception.getMessage().contains("Source card is not active"));
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_DestinationCardNotOwnedByUser_ThrowsException() {
        Long userId = 1L;
        Long otherUserId = 2L;
        TransferRequestDto request = new TransferRequestDto(
                1L,
                2L,
                new BigDecimal("100.00"),
                "Test transfer"
        );

        User user = User.builder().id(userId).username("testuser").build();
        User otherUser = User.builder().id(otherUserId).username("otheruser").build();

        Card fromCard = Card.builder()
                .id(1L)
                .owner(user)
                .balance(new BigDecimal("500.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("1234")
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .owner(otherUser)
                .balance(new BigDecimal("200.00"))
                .status(CardStatus.ACTIVE)
                .expiryDate(LocalDate.now().plusYears(2))
                .lastFourDigits("5678")
                .build();

        when(cardRepository.findById(request.fromCardId())).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(request.toCardId())).thenReturn(Optional.of(toCard));

        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transfer(request, userId)
        );

        assertEquals("Destination card does not belong to user", exception.getMessage());
        verify(cardRepository, never()).save(any(Card.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_FromCardNotFound_ThrowsException() {
        Long userId = 1L;
        TransferRequestDto request = new TransferRequestDto(
                999L,
                2L,
                new BigDecimal("100.00"),
                "Test transfer"
        );

        when(cardRepository.findById(request.fromCardId())).thenReturn(Optional.empty());

        CardNotFoundException exception = assertThrows(
                CardNotFoundException.class,
                () -> transferService.transfer(request, userId)
        );

        assertEquals("Card not found with id: 999", exception.getMessage());
        verify(cardRepository).findById(request.fromCardId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistory_ReturnsPageOfTransactions() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = User.builder().id(userId).username("testuser").build();

        Card fromCard = Card.builder()
                .id(1L)
                .owner(user)
                .lastFourDigits("1234")
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .owner(user)
                .lastFourDigits("5678")
                .build();

        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .sourceCard(fromCard)
                .destinationCard(toCard)
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.COMPLETED)
                .description("Transfer 1")
                .build();

        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .sourceCard(fromCard)
                .destinationCard(toCard)
                .amount(new BigDecimal("200.00"))
                .status(TransactionStatus.COMPLETED)
                .description("Transfer 2")
                .build();

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction1, transaction2), pageable, 2);
        when(transactionRepository.findByUserId(userId, pageable)).thenReturn(transactionPage);

        Page<TransactionDto> result = transferService.getTransactionHistory(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(new BigDecimal("100.00"), result.getContent().get(0).amount());
        assertEquals(new BigDecimal("200.00"), result.getContent().get(1).amount());

        verify(transactionRepository).findByUserId(userId, pageable);
    }
}