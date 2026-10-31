package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.config.SimulationSettings;
import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.model.Task;
import cn.learn.lfy.mtr.domain.model.TaskAsset;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.mapper.TaskAssetMapper;
import cn.learn.lfy.mtr.mapper.TaskMapper;
import cn.learn.lfy.mtr.mapper.TemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class TaskExecutionService {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

    private final TaskMapper taskMapper;
    private final TaskAssetMapper taskAssetMapper;
    private final TemplateMapper templateMapper;
    private final UploadStorage uploadStorage;
    private final TaskLifecycleService lifecycleService;
    private final SimulationSettings simulationSettings;

    public TaskExecutionService(
            TaskMapper taskMapper,
            TaskAssetMapper taskAssetMapper,
            TemplateMapper templateMapper,
            UploadStorage uploadStorage,
            TaskLifecycleService lifecycleService,
            SimulationSettings simulationSettings
    ) {
        this.taskMapper = taskMapper;
        this.taskAssetMapper = taskAssetMapper;
        this.templateMapper = templateMapper;
        this.uploadStorage = uploadStorage;
        this.lifecycleService = lifecycleService;
        this.simulationSettings = simulationSettings;
    }

    public void execute(String taskId) {
        TaskStage activeStage = TaskStage.QUEUED;
        try {
            activeStage = TaskStage.ASSET_CHECK;
            runStage(taskId, activeStage, () -> {
                checkAssets(taskId);
                return null;
            });

            activeStage = TaskStage.SIMULATING;
            String generatedUrl = runStage(taskId, activeStage, () -> simulateGeneration(taskId));

            activeStage = TaskStage.RESULT_PACKAGING;
            String resultUrl = runStage(taskId, activeStage, () -> packageResult(generatedUrl));
            lifecycleService.succeed(taskId, resultUrl);
        } catch (SimulatedStageFailure failure) {
            log.info("任务 {} 在阶段 {} 模拟失败", taskId, failure.stage());
            lifecycleService.fail(taskId, failure.stage(), failure.getMessage());
        } catch (TaskStoppedException exception) {
            log.debug("任务已处于终态，停止后续处理：{}", taskId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            lifecycleService.fail(taskId, activeStage, "任务处理被中断");
        } catch (TaskExecutionException exception) {
            log.warn("任务处理失败：{}，阶段：{}，原因：{}", taskId, activeStage, exception.getMessage());
            lifecycleService.fail(taskId, activeStage, exception.getMessage());
        } catch (Exception exception) {
            log.error("任务处理异常：{}，阶段：{}", taskId, activeStage, exception);
            lifecycleService.fail(taskId, activeStage, "任务处理失败，请稍后重试");
        }
    }

    private <T> T runStage(String taskId, TaskStage stage, Supplier<T> action) throws InterruptedException {
        if (!lifecycleService.enterStage(taskId, stage)) {
            throw new TaskStoppedException();
        }

        pauseForStep();
        if (simulationSettings.failStage() == stage) {
            throw new SimulatedStageFailure(stage, "模拟失败：" + stageLabel(stage));
        }

        T result = action.get();
        if (!lifecycleService.finishStage(taskId, stage)) {
            throw new TaskStoppedException();
        }
        return result;
    }

    private void checkAssets(String taskId) {
        Task task = requireTask(taskId);
        Template template = requireTemplate(task.templateId());
        List<TaskAsset> assets = taskAssetMapper.findByTaskId(taskId);
        if (assets.size() != template.requiredImageCount()) {
            throw new TaskExecutionException("任务素材数量不完整");
        }

        for (TaskAsset asset : assets) {
            String mimeType = normalizeMimeType(asset.mimeType());
            if (template.allowedMimeTypes().stream()
                    .map(TaskExecutionService::normalizeMimeType)
                    .noneMatch(mimeType::equals)) {
                throw new TaskExecutionException("第 " + (asset.sortOrder() + 1) + " 张素材格式不符合要求");
            }
            if (asset.sizeBytes() == null || asset.sizeBytes() <= 0
                    || asset.sizeBytes() > template.maxImageSizeBytes()) {
                throw new TaskExecutionException("第 " + (asset.sortOrder() + 1) + " 张素材大小不符合要求");
            }
            if (!uploadStorage.containsFile(asset.storageKey(), asset.sizeBytes())) {
                throw new TaskExecutionException("第 " + (asset.sortOrder() + 1) + " 张素材文件不存在或已损坏");
            }
        }
    }

    private String simulateGeneration(String taskId) {
        Template template = requireTemplate(requireTask(taskId).templateId());
        return template.exampleImageUrl() + "?taskId=" + taskId;
    }

    private static String packageResult(String generatedUrl) {
        if (generatedUrl == null || generatedUrl.isBlank() || generatedUrl.length() > 500) {
            throw new TaskExecutionException("任务结果地址无效");
        }
        return generatedUrl;
    }

    private Task requireTask(String taskId) {
        return taskMapper.findById(taskId)
                .orElseThrow(() -> new TaskExecutionException("任务不存在"));
    }

    private Template requireTemplate(long templateId) {
        return templateMapper.findById(templateId)
                .orElseThrow(() -> new TaskExecutionException("模板不存在"));
    }

    private void pauseForStep() throws InterruptedException {
        if (simulationSettings.stepDelayMs() > 0) {
            Thread.sleep(simulationSettings.stepDelayMs());
        }
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null) return "";
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
    }

    private static String stageLabel(TaskStage stage) {
        return switch (stage) {
            case ASSET_CHECK -> "素材检查";
            case SIMULATING -> "模拟生成";
            case RESULT_PACKAGING -> "结果整理";
            default -> stage.name();
        };
    }

    private static class TaskExecutionException extends RuntimeException {
        TaskExecutionException(String message) {
            super(message);
        }
    }

    private static class TaskStoppedException extends RuntimeException {
    }

    private static class SimulatedStageFailure extends RuntimeException {
        private final TaskStage stage;

        SimulatedStageFailure(TaskStage stage, String message) {
            super(message);
            this.stage = stage;
        }

        TaskStage stage() {
            return stage;
        }
    }
}
