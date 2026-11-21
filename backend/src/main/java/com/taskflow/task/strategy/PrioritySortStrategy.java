package com.taskflow.task.strategy;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PrioritySortStrategy implements TaskSortStrategy {

    @Override
    public boolean supports(String sortBy) {
        return sortBy != null && sortBy.equalsIgnoreCase("priority");
    }

    @Override
    public Sort getSort(Sort.Direction direction) {
        return Sort.by(direction, "priorityRank").and(Sort.by(direction, "id"));
    }
}
