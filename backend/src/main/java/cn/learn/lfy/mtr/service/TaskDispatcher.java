package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.event.TaskSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TaskDispatcher {
    private static final Logger log = LoggerFactory.getLogger(TaskDispatcher.class);

    private final ThreadPoolTaskExecutor taskExecutionExecutor;
    private final TaskExecutionService taskExecutionService;
    private final TaskLifecycleService lifecycleService;

    public TaskDispatcher(
            @Qualifier("taskExecutionExecutor") ThreadPoolTaskExecutor taskExecutionExecutor,
            TaskExecutionService taskExecutionService,
            TaskLifecycleService lifecycleService
    ) {
        this.taskExecutionExecutor = taskExecutionExecutor;
        this.taskExecutionService = taskExecutionService;
        this.lifecycleService = lifecycleService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskSubmitted(TaskSubmittedEvent event) {
        try {
            taskExecutionExecutor.execute(() -> taskExecutionService.execute(event.taskId()));
        } catch (TaskRejectedException exception) {
            log.warn("任务队列已满，任务提交失败：{}", event.taskId(), exception);
            try {
                lifecycleService.fail(
                        event.taskId(),
                        TaskStage.QUEUED,
                        "系统繁忙，任务未能进入执行队列，请稍后重新提交"
                );
            } catch (RuntimeException updateException) {
                log.error("写入任务队列失败状态时异常：{}", event.taskId(), updateException);
            }
        }
    }
}
