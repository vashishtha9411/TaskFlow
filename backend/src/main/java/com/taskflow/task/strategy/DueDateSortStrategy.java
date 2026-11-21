package com.taskflow.task.strategy;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class DueDateSortStrategy implements TaskSortStrategy {

    @Override
    public boolean supports(String sortBy) {
        return sortBy == null || sortBy.equalsIgnoreCase("dueDate") || sortBy.equalsIgnoreCase("due_date");
    }

    @Override
    public Sort getSort(Sort.Direction direction) {
        return Sort.by(direction, "dueDate").and(Sort.by(direction, "id"));
    }
}
