package com.taskflow.project.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private OffsetDateTime createdAt;
    private long taskCount;
}
