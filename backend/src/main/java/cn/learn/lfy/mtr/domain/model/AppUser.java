package cn.learn.lfy.mtr.domain.model;

import java.time.LocalDateTime;

public record AppUser(Long id, String nickname, LocalDateTime createdAt) {
}
