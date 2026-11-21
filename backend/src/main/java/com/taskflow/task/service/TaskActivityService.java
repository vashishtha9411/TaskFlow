package com.taskflow.task.service;

import com.taskflow.task.model.TaskActivity;
import com.taskflow.task.model.TaskStatus;
import com.taskflow.auth.model.User;
import com.taskflow.task.model.Task;
import com.taskflow.task.repository.TaskActivityRepository;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.task.dto.TaskActivityResponse;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.exception.TaskFlowAccessDeniedException;
import com.taskflow.task.mapper.TaskActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskActivityService {

    private final TaskActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final TaskActivityMapper activityMapper;

    @Transactional
    public void logActivity(Task task, User user, String action, TaskStatus oldStatus, TaskStatus newStatus) {
        TaskActivity activity = TaskActivity.builder()
                .task(task)
                .user(user)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();
        activityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<TaskActivityResponse> getActivitiesForTask(Long taskId, Long currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        if (!task.getProject().getOwner().getId().equals(currentUserId)) {
            throw new TaskFlowAccessDeniedException("You do not have permission to view activities for this task.");
        }

        return activityRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(activityMapper::toResponse)
                .collect(Collectors.toList());
    }
}
