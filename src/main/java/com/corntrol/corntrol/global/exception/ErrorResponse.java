package com.corntrol.corntrol.global.exception;

public record ErrorResponse(
        int status,
        String error,
        String message
) {
}
