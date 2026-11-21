package com.taskflow.auth.controller;

import com.taskflow.auth.dto.AssigneeResponse;
import com.taskflow.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "Endpoints for user directory and assignments")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all registered users", description = "Used for task assignee selection")
    public ResponseEntity<List<AssigneeResponse>> getUsers() {
        List<AssigneeResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
