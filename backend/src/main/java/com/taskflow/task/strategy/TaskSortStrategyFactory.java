package com.taskflow.task.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for resolving the appropriate TaskSortStrategy.
 */
@Component
@RequiredArgsConstructor
public class TaskSortStrategyFactory {

    private final List<TaskSortStrategy> strategies;
    private final DueDateSortStrategy defaultStrategy;

    public Sort getSort(String sortBy, String directionStr) {
        Sort.Direction direction = "desc".equalsIgnoreCase(directionStr)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        TaskSortStrategy selectedStrategy = strategies.stream()
                .filter(strategy -> strategy.supports(sortBy))
                .findFirst()
                .orElse(defaultStrategy);

        return selectedStrategy.getSort(direction);
    }
}
