package com.taskflow.task.strategy;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CreatedAtSortStrategy implements TaskSortStrategy {

    @Override
    public boolean supports(String sortBy) {
        return sortBy != null && (sortBy.equalsIgnoreCase("createdAt") || sortBy.equalsIgnoreCase("created_at"));
    }

    @Override
    public Sort getSort(Sort.Direction direction) {
        return Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    }
}
