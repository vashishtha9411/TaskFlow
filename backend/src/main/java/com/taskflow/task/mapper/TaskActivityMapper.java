package com.taskflow.task.mapper;

import com.taskflow.task.model.TaskActivity;
import com.taskflow.task.dto.TaskActivityResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskActivityMapper {

    public TaskActivityResponse toResponse(TaskActivity activity) {
        if (activity == null) {
            return null;
        }
        return TaskActivityResponse.builder()
                .id(activity.getId())
                .taskId(activity.getTask() != null ? activity.getTask().getId() : null)
                .userId(activity.getUser() != null ? activity.getUser().getId() : null)
                .userName(activity.getUser() != null ? activity.getUser().getName() : null)
                .action(activity.getAction())
                .oldStatus(activity.getOldStatus())
                .newStatus(activity.getNewStatus())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
