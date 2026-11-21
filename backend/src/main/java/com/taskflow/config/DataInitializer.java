package com.taskflow.config;

import com.taskflow.auth.model.User;
import com.taskflow.project.model.Project;
import com.taskflow.task.model.*;
import com.taskflow.project.repository.ProjectRepository;
import com.taskflow.task.repository.TaskActivityRepository;
import com.taskflow.task.repository.TaskRepository;
import com.taskflow.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${TASKFLOW_DEMO_PASSWORD}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (demoPassword.isBlank() || demoPassword.length() < 6 || demoPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set TASKFLOW_DEMO_PASSWORD to at least 6 characters and at most 72 UTF-8 bytes.");
        }
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial demo data creation.");
            return;
        }

        log.info("Seeding initial demo data for TaskFlow...");

        // 1. Create Demo Users
        User demoAdmin = userRepository.save(User.builder()
                .name("Demo Admin")
                .email("demo@taskflow.com")
                .password(passwordEncoder.encode(demoPassword))
                .build());

        User sarah = userRepository.save(User.builder()
                .name("Sarah Jenkins")
                .email("sarah@taskflow.com")
                .password(passwordEncoder.encode(demoPassword))
                .build());

        User alex = userRepository.save(User.builder()
                .name("Alex Rivera")
                .email("alex@taskflow.com")
                .password(passwordEncoder.encode(demoPassword))
                .build());

        // 2. Create Projects
        Project platformProject = projectRepository.save(Project.builder()
                .name("TaskFlow Platform V1")
                .description("Core product features, dashboard, Kanban board, and REST API development.")
                .owner(demoAdmin)
                .build());

        projectRepository.save(Project.builder()
                .name("Growth & Marketing Q4")
                .description("Product launch campaigns, landing page redesign, and customer outreach.")
                .owner(demoAdmin)
                .build());

        // 3. Create Tasks for platformProject
        LocalDate today = LocalDate.now();

        Task t1 = taskRepository.save(Task.builder()
                .title("Implement JWT Authentication & Spring Security 6")
                .description("Build stateless authentication with JWT tokens, refresh handling, and password encryption.")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.HIGH)
                .dueDate(today.minusDays(2))
                .assignee(demoAdmin)
                .project(platformProject)
                .build());

        Task t2 = taskRepository.save(Task.builder()
                .title("Design Interactive Kanban Board & Drag-Drop")
                .description("Build responsive three-column board (To Do, In Progress, Done) with status transitions.")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(today.plusDays(2))
                .assignee(sarah)
                .project(platformProject)
                .build());

        Task t3 = taskRepository.save(Task.builder()
                .title("Security Audit & Input Sanitization")
                .description("Review API request bounds, XSS mitigation, and Bean Validation constraints.")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(today.minusDays(3)) // Overdue task to test overdue logic!
                .assignee(demoAdmin)
                .project(platformProject)
                .build());

        taskRepository.save(Task.builder()
                .title("Setup PostgreSQL & Flyway Schema Migrations")
                .description("Configure automated schema migrations and JPA entity validation.")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.MEDIUM)
                .dueDate(today.minusDays(1))
                .assignee(alex)
                .project(platformProject)
                .build());

        taskRepository.save(Task.builder()
                .title("Write Comprehensive Unit & Integration Tests")
                .description("Test service layer, MockMvc controllers, and project ownership rules.")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(today.plusDays(1))
                .assignee(demoAdmin)
                .project(platformProject)
                .build());

        taskRepository.save(Task.builder()
                .title("Optimize Mobile Responsiveness & Touch Controls")
                .description("Ensure sidebar collapsing and board scrolling feel native on mobile devices.")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .dueDate(today.plusDays(5))
                .assignee(sarah)
                .project(platformProject)
                .build());

        // 4. Seed Audit Activities
        taskActivityRepository.save(TaskActivity.builder()
                .task(t1)
                .user(demoAdmin)
                .action("STATUS_CHANGED")
                .oldStatus(TaskStatus.IN_PROGRESS)
                .newStatus(TaskStatus.DONE)
                .build());

        taskActivityRepository.save(TaskActivity.builder()
                .task(t2)
                .user(sarah)
                .action("STATUS_CHANGED")
                .oldStatus(TaskStatus.TODO)
                .newStatus(TaskStatus.IN_PROGRESS)
                .build());

        taskActivityRepository.save(TaskActivity.builder()
                .task(t3)
                .user(demoAdmin)
                .action("CREATED")
                .oldStatus(null)
                .newStatus(TaskStatus.TODO)
                .build());

        log.info("Demo data initialized successfully! Sample user: demo@taskflow.com");
    }
}
