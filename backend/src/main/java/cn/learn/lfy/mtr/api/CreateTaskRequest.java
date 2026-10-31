package cn.learn.lfy.mtr.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTaskRequest(
        @NotNull(message = "请选择模板") Long templateId,
        @NotEmpty(message = "请上传所需素材") List<@NotBlank(message = "素材 ID 不能为空") String> assetIds
) {
}
