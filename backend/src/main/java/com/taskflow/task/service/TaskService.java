package com.taskflow.task.service;

import com.taskflow.project.service.ProjectService;

import com.taskflow.auth.model.User;
import com.taskflow.project.model.Project;
import com.taskflow.task.model.*;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.task.repository.TaskSpecification;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.dto.common.PagedResponse;
import com.taskflow.task.dto.*;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.exception.TaskFlowAccessDeniedException;
import com.taskflow.task.mapper.TaskMapper;
import com.taskflow.task.strategy.TaskSortStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final TaskActivityService taskActivityService;
    private final TaskSortStrategyFactory sortStrategyFactory;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> getTasks(
            Long projectId,
            TaskFilterCriteria criteria,
            String sortBy,
            String direction,
            int page,
            int size,
            Long currentUserId
    ) {
        // Enforce strict project ownership
        projectService.getProjectEntityWithOwnershipCheck(projectId, currentUserId);

        // Resolve sort using Strategy Pattern
        Sort sort = sortStrategyFactory.getSort(sortBy, direction);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sort);

        Page<Task> taskPage = taskRepository.findAll(TaskSpecification.withFilters(projectId, criteria), pageable);
        return PagedResponse.of(taskPage.map(taskMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId, Long currentUserId) {
        return taskMapper.toResponse(getTaskEntityWithOwnershipCheck(taskId, currentUserId));
    }

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request, Long currentUserId) {
        Project project = projectService.getProjectEntityWithOwnershipCheck(projectId, currentUserId);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        // Validate due date not in the past
        if (request.getDueDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Task due date must be today or in the future.");
        }

        User assignee = null;
        if (request.getAssigneeId() != null && request.getAssigneeId() > 0) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found with id: " + request.getAssigneeId()));
        }

        Task task = taskMapper.toEntity(request, project, assignee);
        Task savedTask = taskRepository.save(task);

        // Record creation activity
        taskActivityService.logActivity(savedTask, currentUser, "CREATED", null, savedTask.getStatus());
        log.info("Task id {} created in project {}", savedTask.getId(), projectId);

        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long currentUserId) {
        Task task = getTaskEntityWithOwnershipCheck(taskId, currentUserId);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        TaskStatus oldStatus = task.getStatus();
        boolean statusChanged = oldStatus != request.getStatus();

        User assignee = null;
        if (request.getAssigneeId() != null && request.getAssigneeId() > 0) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found with id: " + request.getAssigneeId()));
        }

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignee(assignee);

        Task updatedTask = taskRepository.save(task);

        if (statusChanged) {
            taskActivityService.logActivity(updatedTask, currentUser, "STATUS_CHANGED", oldStatus, request.getStatus());
        } else {
            taskActivityService.logActivity(updatedTask, currentUser, "UPDATED", null, null);
        }

        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, TaskStatus newStatus, Long currentUserId) {
        Task task = getTaskEntityWithOwnershipCheck(taskId, currentUserId);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        TaskStatus oldStatus = task.getStatus();
        if (oldStatus != newStatus) {
            task.setStatus(newStatus);
            Task updatedTask = taskRepository.save(task);
            taskActivityService.logActivity(updatedTask, currentUser, "STATUS_CHANGED", oldStatus, newStatus);
            log.info("Task {} status changed from {} to {}", taskId, oldStatus, newStatus);
            return taskMapper.toResponse(updatedTask);
        }

        return taskMapper.toResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        Task task = getTaskEntityWithOwnershipCheck(taskId, currentUserId);
        taskRepository.delete(task);
        log.info("Task {} deleted by user {}", taskId, currentUserId);
    }

    @Transactional(readOnly = true)
    public Task getTaskEntityWithOwnershipCheck(Long taskId, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if (!Objects.equals(task.getProject().getOwner().getId(), currentUserId)) {
            log.warn("Access denied: User {} tried to access task {} belonging to project owner {}",
                    currentUserId, taskId, task.getProject().getOwner().getId());
            throw new TaskFlowAccessDeniedException("You do not have permission to access or modify this task.");
        }
        return task;
    }
}
