package com.taskflow.task.mapper;

import com.taskflow.project.model.Project;
import com.taskflow.task.model.Task;
import com.taskflow.task.model.TaskStatus;
import com.taskflow.auth.model.User;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final UserMapper userMapper;

    public Task toEntity(CreateTaskRequest request, Project project, User assignee) {
        return Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .assignee(assignee)
                .project(project)
                .build();
    }

    public TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }
        boolean isOverdue = task.getStatus() != TaskStatus.DONE &&
                task.getDueDate() != null &&
                task.getDueDate().isBefore(LocalDate.now());

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .overdue(isOverdue)
                .assignee(userMapper.toAssignee(task.getAssignee()))
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
