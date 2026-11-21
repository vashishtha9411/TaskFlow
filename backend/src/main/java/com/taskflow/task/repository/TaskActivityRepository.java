package com.taskflow.task.repository;

import com.taskflow.task.model.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

    @Query("SELECT a FROM TaskActivity a WHERE a.task.id = :taskId ORDER BY a.createdAt DESC")
    List<TaskActivity> findAllByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);
}
