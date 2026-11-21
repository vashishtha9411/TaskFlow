package com.taskflow.task.dto;

import com.taskflow.task.model.TaskStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskActivityResponse {
    private Long id;
    private Long taskId;
    private Long userId;
    private String userName;
    private String action;
    private TaskStatus oldStatus;
    private TaskStatus newStatus;
    private OffsetDateTime createdAt;
}
