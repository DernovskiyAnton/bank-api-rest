package com.example.bankcards.entity;

public enum TransactionStatus {

    /**
     * Транзакция создана, но еще не обработана.
     */
    PENDING,

    /**
     * Транзакция выполняется.
     */
    PROCESSING,

    /**
     * Транзакция успешно завершена.
     */
    COMPLETED,

    /**
     * Транзакция не удалась.
     */
    FAILED,

    /**
     * Транзакция отменена.
     */
    CANCELLED
}