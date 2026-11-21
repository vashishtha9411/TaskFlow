package com.taskflow.task.dto;

import com.taskflow.task.model.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
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
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 1, max = 120, message = "Task title cannot exceed 120 characters")
    private String title;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be on or after today")
    private LocalDate dueDate;

    private Long assigneeId;
}
