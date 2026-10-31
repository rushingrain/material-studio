package cn.learn.lfy.mtr.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SimulationConfigRequest(
        String failStage,
        @NotNull(message = "请填写素材检查阶段耗时")
        @Min(value = 0, message = "素材检查阶段耗时不能小于 0")
        Long assetCheckDelayMs,
        @NotNull(message = "请填写模拟生成阶段耗时")
        @Min(value = 0, message = "模拟生成阶段耗时不能小于 0")
        Long simulatingDelayMs,
        @NotNull(message = "请填写结果整理阶段耗时")
        @Min(value = 0, message = "结果整理阶段耗时不能小于 0")
        Long resultPackagingDelayMs
) {
}
