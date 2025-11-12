package com.example.bankcards.exception;


public class CardBlockedException extends RuntimeException{

    public CardBlockedException(Long cardId) {
        super("Card with id: " + cardId + " is blocked");
    }

    public CardBlockedException(Long cardId, String blockReason) {
        super("Card with id: " + cardId + " is blocked. Reason: " + blockReason);
    }
}
