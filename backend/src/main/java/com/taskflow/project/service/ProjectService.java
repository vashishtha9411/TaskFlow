package com.taskflow.project.service;

import com.taskflow.project.model.Project;
import com.taskflow.auth.model.User;
import com.taskflow.project.repository.ProjectRepository;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.auth.repository.UserRepository;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectResponse;
import com.taskflow.project.dto.UpdateProjectRequest;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.exception.TaskFlowAccessDeniedException;
import com.taskflow.project.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Long currentUserId) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        Project project = projectMapper.toEntity(request, owner);
        Project saved = projectRepository.save(project);
        log.info("Project '{}' (id: {}) created by user {}", saved.getName(), saved.getId(), currentUserId);
        return projectMapper.toResponse(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsForUser(Long currentUserId) {
        List<Project> projects = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUserId);
        return projects.stream()
                .map(p -> {
                    long taskCount = taskRepository.countByProjectId(p.getId());
                    return projectMapper.toResponse(p, taskCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, Long currentUserId) {
        Project project = getProjectEntityWithOwnershipCheck(projectId, currentUserId);
        long taskCount = taskRepository.countByProjectId(project.getId());
        return projectMapper.toResponse(project, taskCount);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request, Long currentUserId) {
        Project project = getProjectEntityWithOwnershipCheck(projectId, currentUserId);
        project.setName(request.getName().trim());
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription().trim());
        } else {
            project.setDescription(null);
        }
        Project updated = projectRepository.save(project);
        long taskCount = taskRepository.countByProjectId(updated.getId());
        return projectMapper.toResponse(updated, taskCount);
    }

    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        Project project = getProjectEntityWithOwnershipCheck(projectId, currentUserId);
        projectRepository.delete(project);
        log.info("Project id {} deleted by user {}", projectId, currentUserId);
    }

    @Transactional(readOnly = true)
    public Project getProjectEntityWithOwnershipCheck(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.getOwner().getId().equals(currentUserId)) {
            log.warn("Access denied: User {} tried to access project {} owned by {}",
                    currentUserId, projectId, project.getOwner().getId());
            throw new TaskFlowAccessDeniedException("You do not have permission to access or modify this project.");
        }
        return project;
    }
}
