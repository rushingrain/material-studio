package cn.learn.lfy.mtr.domain.model;

import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record Task(
        String id,
        Long userId,
        Long templateId,
        TaskStatus status,
        TaskStage stage,
        TaskStage failureStage,
        String failureReason,
        String resultUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
