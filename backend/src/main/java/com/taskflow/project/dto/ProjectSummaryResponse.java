package com.taskflow.project.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSummaryResponse {
    private Long projectId;
    private String projectName;
    private long totalTasks;
    private long todoCount;
    private long inProgressCount;
    private long doneCount;
    private long overdueCount;
    private double completionPercentage;
}
