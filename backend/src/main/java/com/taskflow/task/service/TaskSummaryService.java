package com.taskflow.task.service;

import com.taskflow.project.service.ProjectService;

import com.taskflow.project.model.Project;
import com.taskflow.task.model.TaskStatus;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.project.dto.ProjectSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TaskSummaryService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public ProjectSummaryResponse getProjectSummary(Long projectId, Long currentUserId) {
        // Enforce strict project ownership
        Project project = projectService.getProjectEntityWithOwnershipCheck(projectId, currentUserId);

        long todoCount = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO);
        long inProgressCount = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS);
        long doneCount = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        long totalTasks = todoCount + inProgressCount + doneCount;
        long overdueCount = taskRepository.countOverdueByProjectId(projectId, LocalDate.now());

        double completionPercentage = totalTasks > 0
                ? Math.round(((double) doneCount / totalTasks) * 1000.0) / 10.0
                : 0.0;

        return ProjectSummaryResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalTasks(totalTasks)
                .todoCount(todoCount)
                .inProgressCount(inProgressCount)
                .doneCount(doneCount)
                .overdueCount(overdueCount)
                .completionPercentage(completionPercentage)
                .build();
    }
}
