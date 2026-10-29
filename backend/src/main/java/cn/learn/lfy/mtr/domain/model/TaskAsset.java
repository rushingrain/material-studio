package cn.learn.lfy.mtr.domain.model;

import java.time.LocalDateTime;

public record TaskAsset(
        Long id,
        String taskId,
        String originalFilename,
        String storageKey,
        String mimeType,
        Long sizeBytes,
        Integer sortOrder,
        LocalDateTime createdAt
) {
}
