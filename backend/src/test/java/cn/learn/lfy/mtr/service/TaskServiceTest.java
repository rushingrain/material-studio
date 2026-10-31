package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.api.CreateTaskRequest;
import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.enums.TaskStatus;
import cn.learn.lfy.mtr.domain.model.Task;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.domain.model.UploadAsset;
import cn.learn.lfy.mtr.mapper.TaskAssetMapper;
import cn.learn.lfy.mtr.mapper.TaskMapper;
import cn.learn.lfy.mtr.mapper.TemplateMapper;
import cn.learn.lfy.mtr.mapper.UploadAssetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAssetMapper taskAssetMapper;
    @Mock
    private TemplateMapper templateMapper;
    @Mock
    private UploadAssetMapper uploadAssetMapper;
    @Mock
    private UploadStorage uploadStorage;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskMapper,
                taskAssetMapper,
                templateMapper,
                uploadAssetMapper,
                uploadStorage,
                eventPublisher
        );
    }

    @Test
    void findsTaskOnlyWithinCurrentUserScope() {
        Task task = task(1L);
        when(taskMapper.findByIdAndUserId("task-1", 1L)).thenReturn(Optional.of(task));

        assertSame(task, taskService.findById("task-1", 1L));

        verify(taskMapper).findByIdAndUserId("task-1", 1L);
        verify(taskMapper, never()).findById("task-1");
    }

    @Test
    void rejectsAssetOwnedByAnotherUser() {
        when(templateMapper.findActiveById(10L)).thenReturn(Optional.of(template()));
        when(uploadAssetMapper.findById("asset-1")).thenReturn(Optional.of(asset(2L)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taskService.submit(
                        new CreateTaskRequest(10L, List.of("asset-1")),
                        1L
                )
        );

        assertEquals(ApiErrorCode.ASSET_NOT_FOUND, exception.getErrorCode());
        verify(taskMapper, never()).insert(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static Task task(long userId) {
        return new Task(
                "task-1",
                userId,
                10L,
                TaskStatus.PROCESSING,
                TaskStage.SIMULATING,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private static UploadAsset asset(long userId) {
        return new UploadAsset(
                "asset-1",
                userId,
                10L,
                "image.png",
                "uploads/image.png",
                "image/png",
                128L,
                LocalDateTime.now()
        );
    }

    private static Template template() {
        return new Template(
                10L,
                "sample",
                "示例模板",
                "示例",
                "示例模板",
                "/assets/cover.svg",
                "/assets/result.svg",
                1,
                List.of("image/png"),
                1024L,
                1,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
