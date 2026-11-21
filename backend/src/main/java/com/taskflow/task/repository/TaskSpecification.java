package com.taskflow.task.repository;

import com.taskflow.task.model.Task;
import com.taskflow.task.dto.TaskFilterCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TaskSpecification {

    public static Specification<Task> withFilters(Long projectId, TaskFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Project ID filter (mandatory)
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (criteria != null) {
                // Status filter
                if (criteria.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
                }

                // Priority filter
                if (criteria.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), criteria.getPriority()));
                }

                // Assignee filter
                if (criteria.getAssigneeId() != null) {
                    if (criteria.getAssigneeId() <= 0) {
                        // Special convention: <= 0 means unassigned
                        predicates.add(cb.isNull(root.get("assignee")));
                    } else {
                        predicates.add(cb.equal(root.get("assignee").get("id"), criteria.getAssigneeId()));
                    }
                }

                // Keyword search
                if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                    String pattern = "%" + criteria.getSearch().trim().toLowerCase() + "%";
                    Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                    Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                    predicates.add(cb.or(titleMatch, descMatch));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
