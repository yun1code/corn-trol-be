package com.corntrol.corntrol.domain.focus.service;

import com.corntrol.corntrol.domain.focus.dto.QuestionRequest;
import com.corntrol.corntrol.domain.focus.dto.QuestionResponse;
import com.corntrol.corntrol.domain.focus.entity.FocusSession;
import com.corntrol.corntrol.domain.focus.repository.CoolingQuestionRepository;
import com.corntrol.corntrol.domain.focus.repository.FocusSessionRepository;
import com.corntrol.corntrol.domain.record.entity.Record;
import com.corntrol.corntrol.domain.record.repository.RecordRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FocusService {

    private final RecordRepository recordRepository;
    private final WebClient webClient;
    private final CoolingQuestionRepository coolingQuestionRepository;
    private final FocusSessionRepository focusSessionRepository;

    public void createFocusQuestions(Long userId, Long recordId, String topic) {
        // 1. 현재 기록 조회
        Record current = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("해당 기록을 찾을 수 없습니다."));

        // 2. 연결된 기록들 조회
        List<Record> connectedRecords = recordRepository.findAllConnectedRecords(recordId);

        // 3. AI 서버 전송용 DTO 조립
        QuestionRequest request = QuestionRequest.builder()
                .userId(userId)
                .recordId(recordId)
                .topic(topic)
                .currentRecord(QuestionRequest.RecordDetail.builder()
                        .content(current.getContent())
                        .keywords(parseKeywords(current.getKeywords()))
                        .build())
                .linkedRecords(connectedRecords.stream()
                        .map(r -> QuestionRequest.RecordDetail.builder()
                                .content(r.getContent())
                                .keywords(parseKeywords(r.getKeywords()))
                                .build())
                        .toList())
                .build();

        // 4. WebClient로 AI 서버 호출 (비동기 전송)
        webClient.post()
                .uri("/api/generate-questions") // AI 팀원이 알려주는 실제 경로로 수정 필요
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();
    }

    // 콤마로 구분된 String 키워드를 List<String>으로 변환
    private List<String> parseKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) return List.of();
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .toList();
    }

    public List<QuestionResponse> getQuestions(Long recordId) {
        return coolingQuestionRepository.findAllByRecordId(recordId).stream()
                .map(q -> new QuestionResponse(q.getId(), q.getQuestionText())) // 엔티티 필드명에 맞춰서 get 메서드 호출
                .toList();
    }

    // 1. 집중 모드 시작 API 로직
    @Transactional
    public Long startFocus(Long userId, Long recordId, Integer duration) {
        FocusSession session = FocusSession.builder()
                .userId(userId)     // 명세서에 추가됨
                .recordId(recordId)
                .duration(duration) // 명세서에 추가됨 (몇 분 집중할 건지)
                .build();

        FocusSession savedSession = focusSessionRepository.save(session);
        return savedSession.getId();
    }

    // 2. 집중 모드 종료 API 로직
    @Transactional
    public void endFocus(Long sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("진행 중인 집중 세션을 찾을 수 없습니다."));

        // session.updateEndTime(LocalDateTime.now()); // 종료 시간 기록
    }
}
