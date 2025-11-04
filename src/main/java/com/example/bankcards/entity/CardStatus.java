package com.example.bankcards.entity;
/**
 * Статусы банковской карты
 */
public enum CardStatus {
    /**
     * Карта активна и может использоваться
     */
    ACTIVE,
    /**
     * Карта заблокирована
     */
    BLOCKED,
    /**
     * Истек срок действия
     */
    EXPIRED
}