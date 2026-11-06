package com.foodconnect.mapper;

import com.foodconnect.dto.UserResponse;
import com.foodconnect.entity.User;
import org.springframework.stereotype.Component;

/**
 * Maps User entities to UserResponse DTOs.
 */
@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
