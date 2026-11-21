package com.taskflow.project.controller;

import com.taskflow.task.model.TaskPriority;
import com.taskflow.task.model.TaskStatus;
import com.taskflow.dto.common.PagedResponse;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectResponse;
import com.taskflow.project.dto.ProjectSummaryResponse;
import com.taskflow.project.dto.UpdateProjectRequest;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskFilterCriteria;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.security.UserPrincipal;
import com.taskflow.project.service.ProjectService;
import com.taskflow.task.service.TaskService;
import com.taskflow.task.service.TaskSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Projects", description = "Endpoints for managing projects and their tasks")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final TaskSummaryService taskSummaryService;

    @GetMapping
    @Operation(summary = "List current user's projects")
    public ResponseEntity<List<ProjectResponse>> getProjects(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProjectResponse> projects = projectService.getProjectsForUser(principal.getId());
        return ResponseEntity.ok(projects);
    }

    @PostMapping
    @Operation(summary = "Create a project")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProjectResponse response = projectService.createProject(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProjectResponse response = projectService.getProjectById(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProjectResponse response = projectService.updateProject(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        projectService.deleteProject(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Get project summary and overdue task metrics")
    public ResponseEntity<ProjectSummaryResponse> getProjectSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProjectSummaryResponse summary = taskSummaryService.getProjectSummary(id, principal.getId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}/tasks")
    @Operation(summary = "List tasks with filters, sorting, and pagination")
    public ResponseEntity<PagedResponse<TaskResponse>> getTasks(
            @PathVariable Long id,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "dueDate") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TaskFilterCriteria criteria = TaskFilterCriteria.builder()
                .status(status)
                .priority(priority)
                .assigneeId(assigneeId)
                .search(search)
                .build();

        PagedResponse<TaskResponse> response = taskService.getTasks(
                id, criteria, sortBy, direction, page, size, principal.getId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/tasks")
    @Operation(summary = "Create task within a project")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TaskResponse response = taskService.createTask(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
