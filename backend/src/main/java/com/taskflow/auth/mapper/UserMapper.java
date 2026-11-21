package com.taskflow.auth.mapper;

import com.taskflow.auth.model.User;
import com.taskflow.auth.dto.AssigneeResponse;
import com.taskflow.auth.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public AssigneeResponse toAssignee(User user) {
        return user == null ? null : new AssigneeResponse(user.getId(), user.getName());
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
