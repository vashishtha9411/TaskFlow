package com.taskflow.project.mapper;

import com.taskflow.project.model.Project;
import com.taskflow.auth.model.User;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(CreateProjectRequest request, User owner) {
        return Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .owner(owner)
                .build();
    }

    public ProjectResponse toResponse(Project project) {
        return toResponse(project, project.getTasks() != null ? project.getTasks().size() : 0);
    }

    public ProjectResponse toResponse(Project project, long taskCount) {
        if (project == null) {
            return null;
        }
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .ownerName(project.getOwner() != null ? project.getOwner().getName() : null)
                .createdAt(project.getCreatedAt())
                .taskCount(taskCount)
                .build();
    }
}
