package com.taskflow.auth.service;

import com.taskflow.auth.repository.UserRepository;
import com.taskflow.auth.dto.AssigneeResponse;
import com.taskflow.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<AssigneeResponse> getAllUsers() {
        return userRepository.findAll(Sort.by("name", "id")).stream()
                .map(userMapper::toAssignee)
                .toList();
    }
}
