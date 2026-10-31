package cn.learn.lfy.mtr.config;

import cn.learn.lfy.mtr.domain.enums.TaskStage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Component
public class SimulationSettings {
    private static final Set<TaskStage> FAILABLE_STAGES = EnumSet.of(
            TaskStage.ASSET_CHECK,
            TaskStage.SIMULATING,
            TaskStage.RESULT_PACKAGING
    );

    private volatile SimulationConfig currentConfig;

    public SimulationSettings(
            @Value("${app.simulation.step-delay-ms}") long stepDelayMs,
            @Value("${app.simulation.asset-check-delay-ms:}") String assetCheckDelayMs,
            @Value("${app.simulation.simulating-delay-ms:}") String simulatingDelayMs,
            @Value("${app.simulation.result-packaging-delay-ms:}") String resultPackagingDelayMs,
            @Value("${app.simulation.fail-stage:}") String failStage
    ) {
        long fallbackDelay = requireNonNegative(stepDelayMs, "app.simulation.step-delay-ms");
        this.currentConfig = new SimulationConfig(
                parseFailStage(failStage),
                parseDelay(assetCheckDelayMs, fallbackDelay, "app.simulation.asset-check-delay-ms"),
                parseDelay(simulatingDelayMs, fallbackDelay, "app.simulation.simulating-delay-ms"),
                parseDelay(resultPackagingDelayMs, fallbackDelay, "app.simulation.result-packaging-delay-ms")
        );
    }

    public SimulationConfig snapshot() {
        return currentConfig;
    }

    public synchronized SimulationConfig update(
            String failStage,
            long assetCheckDelayMs,
            long simulatingDelayMs,
            long resultPackagingDelayMs
    ) {
        SimulationConfig nextConfig = new SimulationConfig(
                parseFailStage(failStage),
                requireNonNegative(assetCheckDelayMs, "assetCheckDelayMs"),
                requireNonNegative(simulatingDelayMs, "simulatingDelayMs"),
                requireNonNegative(resultPackagingDelayMs, "resultPackagingDelayMs")
        );
        currentConfig = nextConfig;
        return nextConfig;
    }

    private static long parseDelay(String value, long fallback, String propertyName) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return requireNonNegative(Long.parseLong(value.trim()), propertyName);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(propertyName + " 必须是非负整数", exception);
        }
    }

    private static long requireNonNegative(long value, String propertyName) {
        if (value < 0) {
            throw new IllegalArgumentException(propertyName + " 不能小于 0");
        }
        return value;
    }

    private static TaskStage parseFailStage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            TaskStage stage = TaskStage.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (!FAILABLE_STAGES.contains(stage)) {
                throw new IllegalArgumentException("模拟失败阶段只支持 ASSET_CHECK、SIMULATING、RESULT_PACKAGING");
            }
            return stage;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "无法识别模拟失败阶段：" + value
                            + "；只支持 ASSET_CHECK、SIMULATING、RESULT_PACKAGING",
                    exception
            );
        }
    }
}
