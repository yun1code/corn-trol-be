package com.corntrol.corntrol.domain.media.controller;

import com.corntrol.corntrol.domain.media.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Media", description = "미디어 파일 업로드 및 STT 처리 API")
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final S3Service s3Service;

    /**
     * 클라이언트로부터 전달받은 파일을 S3에 업로드하고 접근 가능한 URL을 반환
     */
    @Operation(summary = "파일 업로드", description = "MultipartFile을 전달받아 S3 버킷에 저장합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        String fileUrl = s3Service.uploadFile(file);
        return ResponseEntity.ok(fileUrl);
    }
}
