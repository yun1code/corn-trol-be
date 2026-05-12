package com.corntrol.corntrol.domain.media.dto;

public record MediaUploadResponse(
        Long id,
        String url,
        String text
) {
}