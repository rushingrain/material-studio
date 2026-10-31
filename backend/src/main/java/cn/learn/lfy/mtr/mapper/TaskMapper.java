package cn.learn.lfy.mtr.mapper;

import cn.learn.lfy.mtr.domain.enums.TaskStage;
import cn.learn.lfy.mtr.domain.model.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TaskMapper {

    int insert(@Param("task") Task task);

    Optional<Task> findById(@Param("taskId") String taskId);

    Optional<Task> findByIdAndUserId(
            @Param("taskId") String taskId,
            @Param("userId") long userId
    );

    List<Task> findByUserId(@Param("userId") long userId);

    int enterStage(@Param("taskId") String taskId, @Param("stage") TaskStage stage);

    int finishStage(@Param("taskId") String taskId, @Param("stage") TaskStage stage);

    int markSucceeded(@Param("taskId") String taskId, @Param("resultUrl") String resultUrl);

    int markFailed(
            @Param("taskId") String taskId,
            @Param("failureStage") TaskStage failureStage,
            @Param("failureReason") String failureReason
    );
}
