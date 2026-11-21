package com.taskflow.task.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSortStrategyTest {

    @Test
    @DisplayName("DueDateSortStrategy should support dueDate variants and sort by dueDate and id")
    void testDueDateStrategy() {
        DueDateSortStrategy strategy = new DueDateSortStrategy();
        assertThat(strategy.supports("dueDate")).isTrue();
        assertThat(strategy.supports("due_date")).isTrue();
        assertThat(strategy.supports(null)).isTrue();
        assertThat(strategy.supports("priority")).isFalse();

        Sort sort = strategy.getSort(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("dueDate")).isNotNull();
        assertThat(sort.getOrderFor("dueDate").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("PrioritySortStrategy should support priority and sort correctly")
    void testPriorityStrategy() {
        PrioritySortStrategy strategy = new PrioritySortStrategy();
        assertThat(strategy.supports("priority")).isTrue();
        assertThat(strategy.supports("dueDate")).isFalse();

        Sort sort = strategy.getSort(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("priorityRank")).isNotNull();
        assertThat(sort.getOrderFor("priorityRank").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("TaskSortStrategyFactory should resolve correct strategy based on sortBy")
    void testSortStrategyFactory() {
        DueDateSortStrategy dueDateStrategy = new DueDateSortStrategy();
        PrioritySortStrategy priorityStrategy = new PrioritySortStrategy();
        CreatedAtSortStrategy createdAtStrategy = new CreatedAtSortStrategy();

        TaskSortStrategyFactory factory = new TaskSortStrategyFactory(
                List.of(dueDateStrategy, priorityStrategy, createdAtStrategy),
                dueDateStrategy
        );

        Sort prioritySort = factory.getSort("priority", "desc");
        assertThat(prioritySort.getOrderFor("priorityRank")).isNotNull();

        Sort fallbackSort = factory.getSort("unknown_column", "asc");
        assertThat(fallbackSort.getOrderFor("dueDate")).isNotNull();
    }
}
