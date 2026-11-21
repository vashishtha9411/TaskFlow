package com.taskflow.project.service;

import com.taskflow.project.model.Project;
import com.taskflow.auth.model.User;
import com.taskflow.project.repository.ProjectRepository;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectResponse;
import com.taskflow.project.dto.UpdateProjectRequest;
import com.taskflow.exception.TaskFlowAccessDeniedException;
import com.taskflow.project.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private User otherUser;
    private Project project;
    private ProjectResponse projectResponse;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("Owner").email("owner@test.com").build();
        otherUser = User.builder().id(2L).name("Other").email("other@test.com").build();

        project = Project.builder()
                .id(10L)
                .name("Alpha Project")
                .description("Sample description")
                .owner(owner)
                .build();

        projectResponse = ProjectResponse.builder()
                .id(10L)
                .name("Alpha Project")
                .description("Sample description")
                .ownerId(1L)
                .ownerName("Owner")
                .taskCount(3)
                .build();
    }

    @Test
    @DisplayName("Should create project for current user")
    void testCreateProjectSuccess() {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("Alpha Project")
                .description("Sample description")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(projectMapper.toEntity(request, owner)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponse(project, 0)).thenReturn(projectResponse);

        ProjectResponse response = projectService.createProject(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Alpha Project");
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    @DisplayName("Should list projects owned by user with task counts")
    void testGetProjectsForUser() {
        when(projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(project));
        when(taskRepository.countByProjectId(10L)).thenReturn(3L);
        when(projectMapper.toResponse(project, 3L)).thenReturn(projectResponse);

        List<ProjectResponse> results = projectService.getProjectsForUser(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTaskCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should throw TaskFlowAccessDeniedException if user does not own project")
    void testAccessDeniedForNonOwner() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getProjectById(10L, 2L))
                .isInstanceOf(TaskFlowAccessDeniedException.class)
                .hasMessageContaining("You do not have permission");
    }

    @Test
    @DisplayName("Should update project if caller is owner")
    void testUpdateProjectSuccess() {
        UpdateProjectRequest request = UpdateProjectRequest.builder()
                .name("Updated Name")
                .description("Updated Desc")
                .build();

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(taskRepository.countByProjectId(10L)).thenReturn(3L);
        when(projectMapper.toResponse(project, 3L)).thenReturn(projectResponse);

        ProjectResponse response = projectService.updateProject(10L, request, 1L);

        assertThat(response).isNotNull();
        verify(projectRepository).save(project);
    }
}
