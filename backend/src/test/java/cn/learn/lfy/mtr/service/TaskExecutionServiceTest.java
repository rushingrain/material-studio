package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.config.SimulationConfig;
import cn.learn.lfy.mtr.config.SimulationSettings;
import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.enums.TaskStatus;
import cn.learn.lfy.mtr.domain.model.Task;
import cn.learn.lfy.mtr.domain.model.TaskAsset;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.mapper.TaskAssetMapper;
import cn.learn.lfy.mtr.mapper.TaskMapper;
import cn.learn.lfy.mtr.mapper.TemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {
    private static final String TASK_ID = "task-1";
    private static final long TEMPLATE_ID = 10L;

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAssetMapper taskAssetMapper;
    @Mock
    private TemplateMapper templateMapper;
    @Mock
    private UploadStorage uploadStorage;
    @Mock
    private TaskLifecycleService lifecycleService;
    @Mock
    private SimulationSettings simulationSettings;

    private TaskExecutionService executionService;

    @BeforeEach
    void setUp() {
        executionService = new TaskExecutionService(
                taskMapper,
                taskAssetMapper,
                templateMapper,
                uploadStorage,
                lifecycleService,
                simulationSettings
        );
    }

    @Test
    void firstStageFailureStopsBeforeAssetCheck() {
        when(simulationSettings.snapshot())
                .thenReturn(new SimulationConfig(TaskStage.ASSET_CHECK, 0, 0, 0));
        when(lifecycleService.enterStage(TASK_ID, TaskStage.ASSET_CHECK)).thenReturn(true);

        executionService.execute(TASK_ID);

        verify(lifecycleService).fail(
                TASK_ID,
                TaskStage.ASSET_CHECK,
                "模拟失败：素材检查阶段"
        );
        verify(taskMapper, never()).findById(TASK_ID);
        verify(lifecycleService, never()).enterStage(TASK_ID, TaskStage.SIMULATING);
        verify(lifecycleService, never()).succeed(any(), any());
    }

    @Test
    void generationFailureStopsPackagingAndSavesFailureMetadata() {
        stubValidAssets();
        when(simulationSettings.snapshot())
                .thenReturn(new SimulationConfig(TaskStage.SIMULATING, 0, 0, 0));
        when(lifecycleService.enterStage(any(), any(TaskStage.class))).thenReturn(true);
        when(lifecycleService.finishStage(TASK_ID, TaskStage.ASSET_CHECK)).thenReturn(true);

        executionService.execute(TASK_ID);

        verify(lifecycleService).finishStage(TASK_ID, TaskStage.ASSET_CHECK);
        verify(lifecycleService).enterStage(TASK_ID, TaskStage.SIMULATING);
        verify(lifecycleService).fail(
                TASK_ID,
                TaskStage.SIMULATING,
                "模拟失败：模拟生成阶段"
        );
        verify(lifecycleService, never()).enterStage(TASK_ID, TaskStage.RESULT_PACKAGING);
        verify(lifecycleService, never()).succeed(any(), any());
    }

    @Test
    void successfulConfigurationRunsAllStages() {
        stubValidAssets();
        when(simulationSettings.snapshot())
                .thenReturn(new SimulationConfig(null, 0, 0, 0));
        when(lifecycleService.enterStage(any(), any(TaskStage.class))).thenReturn(true);
        when(lifecycleService.finishStage(any(), any(TaskStage.class))).thenReturn(true);

        executionService.execute(TASK_ID);

        verify(lifecycleService).enterStage(TASK_ID, TaskStage.ASSET_CHECK);
        verify(lifecycleService).enterStage(TASK_ID, TaskStage.SIMULATING);
        verify(lifecycleService).enterStage(TASK_ID, TaskStage.RESULT_PACKAGING);
        verify(lifecycleService).succeed(
                TASK_ID,
                "/assets/templates/sample.svg?taskId=" + TASK_ID
        );
        verify(simulationSettings, times(1)).snapshot();
        verify(lifecycleService, never()).fail(any(), any(), any());
    }

    @Test
    void packagingFailureDoesNotMarkTaskAsSucceeded() {
        stubValidAssets();
        when(simulationSettings.snapshot())
                .thenReturn(new SimulationConfig(TaskStage.RESULT_PACKAGING, 0, 0, 0));
        when(lifecycleService.enterStage(any(), any(TaskStage.class))).thenReturn(true);
        when(lifecycleService.finishStage(any(), any(TaskStage.class))).thenReturn(true);

        executionService.execute(TASK_ID);

        verify(lifecycleService).finishStage(TASK_ID, TaskStage.ASSET_CHECK);
        verify(lifecycleService).finishStage(TASK_ID, TaskStage.SIMULATING);
        verify(lifecycleService).enterStage(TASK_ID, TaskStage.RESULT_PACKAGING);
        verify(lifecycleService).fail(
                TASK_ID,
                TaskStage.RESULT_PACKAGING,
                "模拟失败：结果整理阶段"
        );
        verify(lifecycleService, never()).succeed(any(), any());
    }

    private void stubValidAssets() {
        Task task = new Task(
                TASK_ID,
                1L,
                TEMPLATE_ID,
                TaskStatus.PENDING,
                TaskStage.QUEUED,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Template template = new Template(
                TEMPLATE_ID,
                "sample",
                "示例模板",
                "示例",
                "示例模板",
                "/assets/templates/cover.svg",
                "/assets/templates/sample.svg",
                1,
                List.of("image/png"),
                1024L,
                1,
                true,
                null,
                null
        );
        TaskAsset asset = new TaskAsset(
                1L,
                TASK_ID,
                "image.png",
                "uploads/image.png",
                "image/png",
                128L,
                0,
                null
        );

        when(taskMapper.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(templateMapper.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(taskAssetMapper.findByTaskId(TASK_ID)).thenReturn(List.of(asset));
        when(uploadStorage.containsFile("uploads/image.png", 128L)).thenReturn(true);
    }
}
