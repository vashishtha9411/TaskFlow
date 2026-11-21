package com.taskflow.task.controller;

import com.taskflow.task.dto.TaskActivityResponse;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import com.taskflow.security.UserPrincipal;
import com.taskflow.task.service.TaskActivityService;
import com.taskflow.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tasks", description = "Endpoints for updating, modifying status, and deleting tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskActivityService taskActivityService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a task")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(taskService.getTask(id, principal.getId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TaskResponse response = taskService.updateTask(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only task status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TaskResponse response = taskService.updateTaskStatus(id, request.getStatus(), principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        taskService.deleteTask(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Get activity/audit log for a task")
    public ResponseEntity<List<TaskActivityResponse>> getTaskActivities(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TaskActivityResponse> activities = taskActivityService.getActivitiesForTask(id, principal.getId());
        return ResponseEntity.ok(activities);
    }
}
