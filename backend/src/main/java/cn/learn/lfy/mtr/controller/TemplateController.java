package cn.learn.lfy.mtr.controller;

import cn.learn.lfy.mtr.api.ApiResponse;
import cn.learn.lfy.mtr.domain.model.Template;
import cn.learn.lfy.mtr.service.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/templates")
    public ApiResponse<List<Template>> listTemplates(@RequestParam(required = false) String category) {
        return ApiResponse.success(templateService.findActiveTemplates(category));
    }

    @GetMapping("/templates/{id}")
    public ApiResponse<Template> getTemplate(@PathVariable long id) {
        return ApiResponse.success(templateService.findActiveById(id));
    }
}
