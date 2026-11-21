package com.taskflow.task.dto;

import com.taskflow.task.model.TaskPriority;
import com.taskflow.task.model.TaskStatus;
import com.taskflow.auth.dto.AssigneeResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private boolean overdue;
    private AssigneeResponse assignee;
    private Long projectId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
