package com.taskflow.task.strategy;

import org.springframework.data.domain.Sort;

/**
 * Strategy interface for dynamically configuring sort orders for Tasks.
 */
public interface TaskSortStrategy {
    boolean supports(String sortBy);
    Sort getSort(Sort.Direction direction);
}
