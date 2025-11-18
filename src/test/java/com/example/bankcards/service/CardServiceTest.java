package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private CardService cardService;

    @Test
    void createCard_ValidRequest_ReturnsCardDto() {
        Long userId = 1L;
        CreateCardRequestDto request = new CreateCardRequestDto(
                "1234567812345678",
                "John Doe",
                LocalDate.now().plusYears(3)
        );

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("pass")
                .role(Role.USER)
                .build();

        String encryptedNumber = "encrypted_1234567812345678";

        Card savedCard = Card.builder()
                .id(1L)
                .cardNumber(encryptedNumber)
                .lastFourDigits("5678")
                .cardholderName(request.cardholderName())
                .expiryDate(request.expiryDate())
                .owner(user)
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(encryptionUtil.encrypt(request.cardNumber())).thenReturn(encryptedNumber);
        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);

        CardDto result = cardService.createCard(request, userId);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("**** **** **** 5678", result.cardNumber());
        assertEquals("John Doe", result.cardholderName());
        assertEquals(CardStatus.ACTIVE, result.status());
        assertEquals(BigDecimal.ZERO, result.balance());

        verify(userRepository).findById(userId);
        verify(encryptionUtil).encrypt(request.cardNumber());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_UserNotFound_ThrowsException() {
        Long userId = 999L;
        CreateCardRequestDto request = new CreateCardRequestDto(
                "1234567812345678",
                "John Doe",
                LocalDate.now().plusYears(3)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCard(request, userId)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getUserCards_ReturnsPageOfCards() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .role(Role.USER)
                .build();

        Card card1 = Card.builder()
                .id(1L)
                .cardNumber("encrypted1")
                .lastFourDigits("1234")
                .cardholderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(2))
                .owner(user)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        Card card2 = Card.builder()
                .id(2L)
                .cardNumber("encrypted2")
                .lastFourDigits("5678")
                .cardholderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(3))
                .owner(user)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("2000.00"))
                .build();

        Page<Card> cardPage = new PageImpl<>(List.of(card1, card2), pageable, 2);
        when(cardRepository.findByOwner_Id(userId, pageable)).thenReturn(cardPage);

        Page<CardDto> result = cardService.getUserCards(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("**** **** **** 1234", result.getContent().get(0).cardNumber());
        assertEquals("**** **** **** 5678", result.getContent().get(1).cardNumber());

        verify(cardRepository).findByOwner_Id(userId, pageable);
    }

    @Test
    void getCardById_ExistingCard_ReturnsCardDto() {
        Long cardId = 1L;
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .role(Role.USER)
                .build();

        Card card = Card.builder()
                .id(cardId)
                .cardNumber("encrypted")
                .lastFourDigits("1234")
                .cardholderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(2))
                .owner(user)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        when(cardRepository.findByIdAndOwner_Id(cardId, userId)).thenReturn(Optional.of(card));

        CardDto result = cardService.getCardById(cardId, userId);

        assertNotNull(result);
        assertEquals(cardId, result.id());
        assertEquals("**** **** **** 1234", result.cardNumber());

        verify(cardRepository).findByIdAndOwner_Id(cardId, userId);
    }

    @Test
    void getCardById_CardNotFound_ThrowsException() {
        Long cardId = 999L;
        Long userId = 1L;

        when(cardRepository.findByIdAndOwner_Id(cardId, userId)).thenReturn(Optional.empty());

        CardNotFoundException exception = assertThrows(
                CardNotFoundException.class,
                () -> cardService.getCardById(cardId, userId)
        );

        assertEquals("Card not found with id: 999", exception.getMessage());
        verify(cardRepository).findByIdAndOwner_Id(cardId, userId);
    }

    @Test
    void blockCard_ValidRequest_BlocksCard() {
        Long cardId = 1L;
        Long userId = 1L;
        String reason = "Lost card";

        User user = User.builder()
                .id(userId)
                .username("testuser")
                .role(Role.USER)
                .build();

        Card card = Card.builder()
                .id(cardId)
                .cardNumber("encrypted")
                .lastFourDigits("1234")
                .cardholderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(2))
                .owner(user)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        when(cardRepository.findByIdAndOwner_Id(cardId, userId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        cardService.blockCard(cardId, reason, userId);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        assertEquals(reason, card.getBlockReason());
        assertNotNull(card.getBlockedAt());

        verify(cardRepository).findByIdAndOwner_Id(cardId, userId);
        verify(cardRepository).save(card);
    }

    @Test
    void activateCard_ValidRequest_ActivatesCard() {
        Long cardId = 1L;

        Card card = Card.builder()
                .id(cardId)
                .cardNumber("encrypted")
                .lastFourDigits("1234")
                .cardholderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("1000.00"))
                .blockReason("Lost card")
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        cardService.activateCard(cardId);

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        assertNull(card.getBlockReason());
        assertNull(card.getBlockedAt());

        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(card);
    }

    @Test
    void deleteCard_ExistingCard_DeletesCard() {
        Long cardId = 1L;

        when(cardRepository.existsById(cardId)).thenReturn(true);

        cardService.deleteCard(cardId);

        verify(cardRepository).existsById(cardId);
        verify(cardRepository).deleteById(cardId);
    }

    @Test
    void deleteCard_NonExistingCard_ThrowsException() {
        Long cardId = 999L;

        when(cardRepository.existsById(cardId)).thenReturn(false);

        CardNotFoundException exception = assertThrows(
                CardNotFoundException.class,
                () -> cardService.deleteCard(cardId)
        );

        assertEquals("Card not found with id: 999", exception.getMessage());
        verify(cardRepository).existsById(cardId);
        verify(cardRepository, never()).deleteById(any());
    }

    @Test
    void getAllCards_ReturnsPageOfAllCards() {
        Pageable pageable = PageRequest.of(0, 10);

        Card card1 = Card.builder()
                .id(1L)
                .cardNumber("encrypted1")
                .lastFourDigits("1234")
                .cardholderName("John Doe")
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        Card card2 = Card.builder()
                .id(2L)
                .cardNumber("encrypted2")
                .lastFourDigits("5678")
                .cardholderName("Jane Smith")
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("2000.00"))
                .build();

        Page<Card> cardPage = new PageImpl<>(List.of(card1, card2), pageable, 2);
        when(cardRepository.findAll(pageable)).thenReturn(cardPage);

        Page<CardDto> result = cardService.getAllCards(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(CardStatus.ACTIVE, result.getContent().get(0).status());
        assertEquals(CardStatus.BLOCKED, result.getContent().get(1).status());

        verify(cardRepository).findAll(pageable);
    }
}
