package com.salon.backend.Repositories.Moderation;

import com.salon.backend.Entities.moderation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ModerationReportRepo extends JpaRepository<ModerationReport, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, ReportTargetType type, Long targetId, ReportStatus status);
    List<ModerationReport> findAllByOrderByCreatedAtDesc();
    List<ModerationReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}
