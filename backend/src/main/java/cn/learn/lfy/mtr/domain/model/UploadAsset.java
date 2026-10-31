package cn.learn.lfy.mtr.domain.model;

import java.time.LocalDateTime;

public record UploadAsset(
        String id,
        Long userId,
        Long templateId,
        String originalFilename,
        String storageKey,
        String mimeType,
        Long sizeBytes,
        LocalDateTime createdAt
) {
}
