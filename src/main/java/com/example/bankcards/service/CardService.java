package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UnauthorizedAccessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    @Transactional
    public CardDto createCard(CreateCardRequestDto request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String encryptedNumber = encryptionUtil.encrypt(request.cardNumber());

        String lastFour = request.cardNumber().substring(12);

        Card card = Card.builder()
                .cardNumber(encryptedNumber)
                .lastFourDigits(lastFour)
                .cardholderName(request.cardholderName())
                .expiryDate(request.expiryDate())
                .owner(user)
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        Card saved = cardRepository.save(card);

        return mapToDto(saved);
    }

    public Page<CardDto> getUserCards(Long userId, Pageable pageable) {
        Page<Card> cards = cardRepository.findByOwner_Id(userId, pageable);
        return cards.map(this::mapToDto);
    }

    public CardDto getCardById(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndOwner_Id(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        return mapToDto(card);
    }

    @Transactional
    public void blockCard(Long cardId, String reason, Long userId) {
        Card card = cardRepository.findByIdAndOwner_Id(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        card.block(reason);
        cardRepository.save(card);
    }

    @Transactional
    public void activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        card.activate();
        cardRepository.save(card);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }

        cardRepository.deleteById(cardId);
    }

    private CardDto mapToDto(Card card) {
        return new CardDto(
                card.getId(),
                card.getMaskedCardNumber(),
                card.getCardholderName(),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance()
        );
    }
}
