package cn.learn.lfy.mtr.mapper;

import cn.learn.lfy.mtr.domain.model.TaskAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskAssetMapper {

    int insert(@Param("asset") TaskAsset asset);

    List<TaskAsset> findByTaskId(@Param("taskId") String taskId);
}
