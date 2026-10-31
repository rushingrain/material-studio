package cn.learn.lfy.mtr.mapper;

import cn.learn.lfy.mtr.domain.model.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TemplateMapper {

    List<Template> findActiveTemplates(@Param("category") String category);

    Optional<Template> findActiveById(@Param("id") long id);
}
