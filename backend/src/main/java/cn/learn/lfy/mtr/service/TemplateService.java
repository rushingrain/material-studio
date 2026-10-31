package cn.learn.lfy.mtr.service;

import cn.learn.lfy.mtr.api.ApiErrorCode;
import cn.learn.lfy.mtr.api.BusinessException;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.mapper.TemplateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateService {
    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public List<Template> findActiveTemplates(String category) {
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return templateMapper.findActiveTemplates(normalizedCategory);
    }

    public Template findActiveById(long id) {
        return templateMapper.findActiveById(id)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.TEMPLATE_NOT_FOUND));
    }
}
