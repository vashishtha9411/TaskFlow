package com.taskflow.task.dto;

import com.taskflow.task.model.TaskPriority;
import com.taskflow.task.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 1, max = 120, message = "Task title cannot exceed 120 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private Long assigneeId;
}
