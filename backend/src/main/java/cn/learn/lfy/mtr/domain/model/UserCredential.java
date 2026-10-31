package cn.learn.lfy.mtr.domain.model;

import java.time.LocalDateTime;

public record UserCredential(
        Long userId,
        String username,
        String passwordHash,
        String passwordSalt,
        Integer passwordIterations,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
