package com.taskflow.controller;

import com.taskflow.dto.*;
import com.taskflow.enums.TaskPriority;
import com.taskflow.enums.TaskStatus;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    // Manual constructor instead of Lombok @RequiredArgsConstructor
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order
    ) {
        String email = authentication.getName();

        List<TaskResponse> tasks = taskService.getTasks(
                email, search, status, priority, sortBy, order
        );

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/summary")
    public ResponseEntity<TaskSummaryResponse> getSummary(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(taskService.getSummary(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(
            Authentication authentication,
            @PathVariable Long id) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                taskService.getTaskById(email, id)
        );
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            Authentication authentication,
            @Valid @RequestBody TaskRequest request
    ) {
        String email = authentication.getName();

        TaskResponse response = taskService.createTask(
                email, request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                taskService.updateTask(email, id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateRequest request
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                taskService.updateTaskStatus(
                        email,
                        id,
                        request.getStatus()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTask(
            Authentication authentication,
            @PathVariable Long id) {

        String email = authentication.getName();

        taskService.deleteTask(email, id);

        return ResponseEntity.ok(
                ApiResponse.success("Task deleted successfully")
        );
    }
}