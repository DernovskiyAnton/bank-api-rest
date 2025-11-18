package com.example.bankcards.entity;

import com.example.bankcards.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сущность банковской карты
 * Номер карты хранится в зашифрованном виде
 */
@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Зашифрованный номер карты
     */
    @Column(name = "card_number_encrypted", nullable = false, unique = true, length = 500)
    private String cardNumber;

    /**
     * Последние 4 цифры номера карты для отображения маски
     */
    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "card_type", length = 20)
    private String cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateStatusBasedOnExpiry();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatusBasedOnExpiry();
    }

    /**
     * Проверяяет срок действия карты и обновляет при необходимости
     */
    private void updateStatusBasedOnExpiry() {
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now()) && status != CardStatus.BLOCKED) {
            status = CardStatus.EXPIRED;
        }
    }

    public String getMaskedCardNumber() {
        return "**** **** **** " + lastFourDigits;
    }

    /**
     * Проверяет, является ли карта активной и валидной для операций.
     */
    public boolean isActiveAndValid() {
        return status == CardStatus.ACTIVE &&
                expiryDate != null &&
                !expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Блокирует карту с указанием причины
     * @param reason
     */
    public void block(String reason) {
        this.status = CardStatus.BLOCKED;
        this.blockedAt = LocalDateTime.now();
        this.blockReason = reason;
    }

    /**
     * Активирует карты(снимает блокировку)
     */
    public void activate() {
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            this.status = CardStatus.EXPIRED;
        } else {
            this.status = CardStatus.ACTIVE;
            this.blockedAt = null;
            this.blockReason = null;
        }
    }

    /**
     * Добавляет средства на баланс.
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Списывает средства с баланса.
     */
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, this.balance);
        }
        this.balance = this.balance.subtract(amount);
    }
}