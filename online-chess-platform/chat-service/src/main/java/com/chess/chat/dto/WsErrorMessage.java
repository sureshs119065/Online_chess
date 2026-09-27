package com.chess.chat.dto;

public record WsErrorMessage(String type, String message) {
    public WsErrorMessage(String message) {
        this("ERROR", message);
    }
}
