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

    private final long stepDelayMs;
    private final TaskStage failStage;

    public SimulationSettings(
            @Value("${app.simulation.step-delay-ms}") long stepDelayMs,
            @Value("${app.simulation.fail-stage:}") String failStage
    ) {
        if (stepDelayMs < 0) {
            throw new IllegalArgumentException("app.simulation.step-delay-ms 不能小于 0");
        }
        this.stepDelayMs = stepDelayMs;
        this.failStage = parseFailStage(failStage);
    }

    public long stepDelayMs() {
        return stepDelayMs;
    }

    public TaskStage failStage() {
        return failStage;
    }

    private static TaskStage parseFailStage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            TaskStage stage = TaskStage.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (!FAILABLE_STAGES.contains(stage)) {
                throw new IllegalArgumentException("app.simulation.fail-stage 只能配置为三步处理阶段之一");
            }
            return stage;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("无法识别 app.simulation.fail-stage: " + value, exception);
        }
    }
}
