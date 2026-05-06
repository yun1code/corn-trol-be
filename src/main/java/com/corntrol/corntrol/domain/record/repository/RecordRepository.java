package com.corntrol.corntrol.domain.record.repository;

import com.corntrol.corntrol.domain.record.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long> {

    // 현재 기록과 연결된 모든 타겟 기록들을 가져오는 쿼리
    @Query("SELECT r FROM Record r " +
            "JOIN RecordLink rl ON r.id = rl.targetRecordId " +
            "WHERE rl.sourceRecordId = :recordId AND rl.isConnected = true")
    List<Record> findAllConnectedRecords(@Param("recordId") Long recordId);
}
