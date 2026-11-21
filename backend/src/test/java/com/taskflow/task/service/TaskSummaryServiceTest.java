package com.taskflow.task.service;

import com.taskflow.project.service.ProjectService;

import com.taskflow.project.model.Project;
import com.taskflow.task.model.TaskStatus;
import com.taskflow.auth.model.User;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.project.dto.ProjectSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSummaryServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private TaskSummaryService summaryService;

    private Project project;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(1L).name("Owner").build();
        project = Project.builder().id(10L).name("Platform V1").owner(owner).build();
    }

    @Test
    @DisplayName("Should calculate counts, overdue tasks, and completion percentage accurately")
    void testGetProjectSummary() {
        when(projectService.getProjectEntityWithOwnershipCheck(10L, 1L)).thenReturn(project);
        when(taskRepository.countByProjectIdAndStatus(10L, TaskStatus.TODO)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatus(10L, TaskStatus.IN_PROGRESS)).thenReturn(3L);
        when(taskRepository.countByProjectIdAndStatus(10L, TaskStatus.DONE)).thenReturn(5L);
        when(taskRepository.countOverdueByProjectId(eq(10L), any(LocalDate.class))).thenReturn(1L);

        ProjectSummaryResponse summary = summaryService.getProjectSummary(10L, 1L);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalTasks()).isEqualTo(10L);
        assertThat(summary.getTodoCount()).isEqualTo(2L);
        assertThat(summary.getInProgressCount()).isEqualTo(3L);
        assertThat(summary.getDoneCount()).isEqualTo(5L);
        assertThat(summary.getOverdueCount()).isEqualTo(1L);
        assertThat(summary.getCompletionPercentage()).isEqualTo(50.0);
    }
}
