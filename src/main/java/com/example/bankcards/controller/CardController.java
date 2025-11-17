package com.example.bankcards.controller;

import com.example.bankcards.dto.BlockCardRequestDto;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequestDto;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Получить свои карты (с пагинацией)
     * USER видит только свои карты
     */
    @GetMapping
    public ResponseEntity<Page<CardDto>> getMyCards(Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<CardDto> cards = cardService.getUserCards(userId, pageable);
        return ResponseEntity.ok(cards);
    }

    /**
     * Получить одну свою карту по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardDto> getCardById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        CardDto card = cardService.getCardById(id, userId);
        return ResponseEntity.ok(card);
    }

    /**
     * Создать новую карту
     */
    @PostMapping
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequestDto request) {
        Long userId = getCurrentUserId();
        CardDto card = cardService.createCard(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * Заблокировать свою карту
     */
    @PutMapping("/{id}/block")
    public ResponseEntity<Void> blockCard(@PathVariable Long id, @Valid @RequestBody BlockCardRequestDto request) {
        Long userId = getCurrentUserId();
        cardService.blockCard(id, request.reason(), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ADMIN: Получить все карты всех пользователей
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDto>> getAllCards(Pageable pageable) {
        Page<CardDto> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    /**
     * ADMIN: Активировать карту
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ADMIN: Удалить карту
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }


}
