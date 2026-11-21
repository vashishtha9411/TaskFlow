package com.taskflow.task.repository;

import com.taskflow.task.model.Task;
import com.taskflow.task.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status != 'DONE' AND t.dueDate < :currentDate")
    long countOverdueByProjectId(@Param("projectId") Long projectId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);
}
