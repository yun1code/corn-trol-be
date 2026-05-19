package com.corntrol.corntrol.domain.connection.service;

import com.corntrol.corntrol.domain.analysis.entity.Analysis;
import com.corntrol.corntrol.domain.analysis.repository.AnalysisRepository;
import com.corntrol.corntrol.domain.connection.dto.*;
import com.corntrol.corntrol.domain.connection.entity.RecordLink;
import com.corntrol.corntrol.domain.connection.repository.RecordLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConnectionService {

    private final AnalysisRepository analysisRepository;
    private final RecordLinkRepository recordLinkRepository;
    private final RestClient restClient;

    public ConnectionService(AnalysisRepository analysisRepository, RecordLinkRepository recordLinkRepository) {
        this.analysisRepository = analysisRepository;
        this.recordLinkRepository = recordLinkRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://corn-trol-ai.onrender.com")
                .build();
    }

    @Transactional
    public AiRecommendResponse recommendTarget(ConnectionRecommendRequest request) {

        Analysis sourceAnalysis = analysisRepository.findTopByRecordIdOrderByIdDesc(request.getRecordId())
                .orElseThrow(() -> new IllegalArgumentException("분석 데이터가 없습니다. 먼저 분석을 진행해주세요. ID: " + request.getRecordId()));

        String targetTopic = sourceAnalysis.getTopic();
        List<RecordLink> existingLinks = recordLinkRepository.findByTopicAndIsConnectedTrue(targetTopic);

        Set<Long> candidateIds = existingLinks.stream()
                .flatMap(link -> Stream.of(link.getSourceRecordId(), link.getTargetRecordId()))
                .filter(id -> !id.equals(sourceAnalysis.getRecordId()))
                .collect(Collectors.toSet());

        List<AiRecordInfo> candidateRecords = candidateIds.stream()
                .map(analysisRepository::findTopByRecordIdOrderByIdDesc)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(a -> new AiRecordInfo(a.getRecordId(), a.getTopic(), a.getKeywords(), a.getEmbedding()))
                .toList();

        // 🚀 [추가된 핵심 로직] 기존 연결 네트워크 후보가 없는 경우 (초기 기록 자동 연결)
        if (candidateRecords.isEmpty()) {
            // 같은 토픽을 가진 다른 기록들을 찾아옴
            List<Analysis> sameTopicRecords = analysisRepository.findByTopicAndRecordIdNot(targetTopic, sourceAnalysis.getRecordId());

            for (Analysis target : sameTopicRecords) {
                Long targetId = target.getRecordId();
                Long sourceId = sourceAnalysis.getRecordId();

                // 1. 소스나 타겟이 이미 다른 네트워크에 속해있는지 검증
                boolean isSourceLinked = recordLinkRepository.existsBySourceRecordIdOrTargetRecordIdAndIsConnectedTrue(sourceId, sourceId);
                boolean isTargetLinked = recordLinkRepository.existsBySourceRecordIdOrTargetRecordIdAndIsConnectedTrue(targetId, targetId);

                // 2. 이 두 기록 간에 이미 연결된 적 있는지 검증
                boolean isPairAlreadyLinked = recordLinkRepository.existsConnectedLinkBetween(sourceId, targetId);

                // 3. 완전히 깨끗한 상태라면 AI를 거치지 않고 냅다 연결!
                if (!isSourceLinked && !isTargetLinked && !isPairAlreadyLinked) {
                    RecordLink autoLink = RecordLink.builder()
                            .userId(Long.valueOf(request.getUserId()))
                            .sourceRecordId(sourceId)
                            .targetRecordId(targetId)
                            .topic(targetTopic)
                            .similarityScore(1.0)
                            .keywordScore(1.0)
                            .finalScore(1.0)
                            .isConnected(true) // 🔥 핵심: 추천 대기가 아니라 진짜로 바로 연결됨!
                            .build();

                    recordLinkRepository.save(autoLink);

                    // 자동 연결되었으므로 여기서 바로 응답 반환하고 메서드 종료 (AI 서버 호출 안 함!)
                    return new AiRecommendResponse(request.getUserId(), sourceId, targetId, targetTopic, 1f, 1f, 1f);
                }
            }
        }
        // 🚀 [여기까지 추가됨]

        AiRecordInfo sourceInfo = new AiRecordInfo(
                sourceAnalysis.getRecordId(),
                sourceAnalysis.getTopic(),
                sourceAnalysis.getKeywords(),
                sourceAnalysis.getEmbedding()
        );

        AiRecommendRequest aiRequest = new AiRecommendRequest(request.getUserId(), sourceInfo, candidateRecords);

        // AI 서버 통신
        AiRecommendResponse response = restClient.post()
                .uri("/connections/recommend")
                .body(aiRequest)
                .retrieve()
                .body(AiRecommendResponse.class);

        if (response != null && response.getTargetRecordId() != null) {
            RecordLink newLink = RecordLink.builder()
                    .userId(Long.valueOf(response.getUserId()))
                    .sourceRecordId(response.getSourceRecordId())
                    .targetRecordId(response.getTargetRecordId())
                    .topic(response.getTopic())
                    .similarityScore((double) response.getSimilarityScore())
                    .keywordScore((double) response.getKeywordScore())
                    .finalScore((double) response.getFinalScore())
                    .isConnected(false) // AI 추천은 사용자가 수락해야 하므로 false 유지
                    .build();

            recordLinkRepository.save(newLink);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public RecordLink getRecommendResult(Long recordId) {
        return recordLinkRepository.findTopBySourceRecordIdAndIsConnectedFalseOrderByIdDesc(recordId)
                .orElseThrow(() -> new IllegalArgumentException("대기 중인 추천 결과가 없습니다."));
    }

    @Transactional
    public Long createConnection(ConnectionCreateRequest request) {
        RecordLink link = recordLinkRepository.findBySourceRecordIdAndTargetRecordId(
                        request.getSourceRecordId(), request.getTargetRecordId())
                .orElseThrow(() -> new IllegalArgumentException("해당 추천 이력이 존재하지 않습니다."));

        link.setConnected(true);
        return link.getId();
    }

    @Transactional(readOnly = true)
    public List<RecordLink> getConnections(Long recordId) {
        return recordLinkRepository.findAllConnectedByRecordId(recordId);
    }

    @Transactional
    public void deleteConnection(Long linkId) {
        if (!recordLinkRepository.existsById(linkId)) {
            throw new IllegalArgumentException("해당 연결이 존재하지 않습니다.");
        }
        recordLinkRepository.deleteById(linkId);
    }
}
