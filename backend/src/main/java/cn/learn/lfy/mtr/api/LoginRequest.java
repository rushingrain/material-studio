package cn.learn.lfy.mtr.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "请输入用户名")
        @Size(max = 80, message = "用户名过长")
        String username,
        @NotBlank(message = "请输入密码")
        @Size(max = 200, message = "密码过长")
        String password
) {
}
