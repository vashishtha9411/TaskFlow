package com.taskflow.task.service;

import com.taskflow.auth.model.User;
import com.taskflow.project.model.Project;
import com.taskflow.task.model.*;
import com.taskflow.project.service.ProjectService;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.TaskFlowAccessDeniedException;
import com.taskflow.task.mapper.TaskMapper;
import com.taskflow.task.strategy.TaskSortStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private TaskActivityService taskActivityService;

    @Mock
    private TaskSortStrategyFactory sortStrategyFactory;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User owner;
    private Project project;
    private Task task;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@test.com").build();

        project = Project.builder()
                .id(10L)
                .name("Platform")
                .owner(owner)
                .build();

        task = Task.builder()
                .id(100L)
                .title("Build API")
                .description("Spring Boot REST API")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .project(project)
                .build();

        taskResponse = TaskResponse.builder()
                .id(100L)
                .title("Build API")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .projectId(10L)
                .build();
    }

    @Test
    @DisplayName("Should create task successfully when caller owns project and due date is valid")
    void testCreateTaskSuccess() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Build API")
                .description("Spring Boot REST API")
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .build();

        when(projectService.getProjectEntityWithOwnershipCheck(10L, 1L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskMapper.toEntity(request, project, null)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        TaskResponse response = taskService.createTask(10L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Build API");
        verify(taskActivityService).logActivity(task, owner, "CREATED", null, TaskStatus.TODO);
    }

    @Test
    @DisplayName("Should throw BadRequestException if due date is in the past on create")
    void testCreateTaskPastDueDateThrowsException() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Build API")
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.now().minusDays(1)) // Past!
                .build();

        when(projectService.getProjectEntityWithOwnershipCheck(10L, 1L)).thenReturn(project);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> taskService.createTask(10L, request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be today or in the future");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should update task status and log activity")
    void testUpdateTaskStatusSuccess() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(taskResponse);

        TaskResponse response = taskService.updateTaskStatus(100L, TaskStatus.IN_PROGRESS, 1L);

        assertThat(response).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskActivityService).logActivity(task, owner, "STATUS_CHANGED", TaskStatus.TODO, TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should throw TaskFlowAccessDeniedException if task is owned by different user")
    void testTaskOwnershipDenied() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateTaskStatus(100L, TaskStatus.DONE, 999L))
                .isInstanceOf(TaskFlowAccessDeniedException.class)
                .hasMessageContaining("You do not have permission");
    }
}
