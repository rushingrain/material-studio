package cn.learn.lfy.mtr.config;

import cn.learn.lfy.mtr.domain.enums.TaskStage;

public record SimulationConfig(
        TaskStage failStage,
        long assetCheckDelayMs,
        long simulatingDelayMs,
        long resultPackagingDelayMs
) {
    public SimulationConfig {
        if (assetCheckDelayMs < 0 || simulatingDelayMs < 0 || resultPackagingDelayMs < 0) {
            throw new IllegalArgumentException("各阶段耗时不能小于 0");
        }
    }

    public long delayFor(TaskStage stage) {
        return switch (stage) {
            case ASSET_CHECK -> assetCheckDelayMs;
            case SIMULATING -> simulatingDelayMs;
            case RESULT_PACKAGING -> resultPackagingDelayMs;
            default -> 0;
        };
    }
}
