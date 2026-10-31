package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.mapper.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskLifecycleService {
    private static final Logger log = LoggerFactory.getLogger(TaskLifecycleService.class);
    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final TaskMapper taskMapper;

    public TaskLifecycleService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public boolean enterStage(String taskId, TaskStage stage) {
        return taskMapper.enterStage(taskId, stage) == 1;
    }

    public boolean finishStage(String taskId, TaskStage stage) {
        return taskMapper.finishStage(taskId, stage) == 1;
    }

    public void succeed(String taskId, String resultUrl) {
        if (taskMapper.markSucceeded(taskId, resultUrl) == 0) {
            log.debug("任务已处于终态，忽略完成写入：{}", taskId);
        }
    }

    public void fail(String taskId, TaskStage failureStage, String failureReason) {
        String reason = failureReason == null || failureReason.isBlank()
                ? "任务处理失败，请稍后重试"
                : failureReason.trim();
        if (reason.length() > MAX_FAILURE_REASON_LENGTH) {
            reason = reason.substring(0, MAX_FAILURE_REASON_LENGTH);
        }
        if (taskMapper.markFailed(taskId, failureStage == null ? TaskStage.QUEUED : failureStage, reason) == 0) {
            log.debug("任务已处于终态，忽略失败写入：{}", taskId);
        }
    }
}
