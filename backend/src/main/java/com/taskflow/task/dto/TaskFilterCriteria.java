package com.taskflow.task.dto;

import com.taskflow.task.model.TaskPriority;
import com.taskflow.task.model.TaskStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskFilterCriteria {
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private String search;
}
