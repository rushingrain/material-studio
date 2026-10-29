package cn.learn.lfy.mtr.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record Template(
        Long id,
        String code,
        String name,
        String category,
        String description,
        String coverUrl,
        String exampleImageUrl,
        Integer requiredImageCount,
        List<String> allowedMimeTypes,
        Long maxImageSizeBytes,
        Integer sortOrder,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
