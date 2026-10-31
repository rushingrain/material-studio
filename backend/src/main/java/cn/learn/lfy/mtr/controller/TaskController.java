package cn.learn.lfy.mtr.controller;

import cn.learn.lfy.mtr.api.ApiResponse;
import cn.learn.lfy.mtr.api.CreateTaskRequest;
import cn.learn.lfy.mtr.api.TaskCreatedResponse;
import cn.learn.lfy.mtr.domain.model.Task;
import cn.learn.lfy.mtr.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ApiResponse<TaskCreatedResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.submit(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Task> get(@PathVariable String id) {
        return ApiResponse.success(taskService.findById(id));
    }
}
