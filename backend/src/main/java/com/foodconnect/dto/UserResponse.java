package com.foodconnect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.foodconnect.entity.enums.AccountStatus;
import com.foodconnect.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for user information.
 * Excludes sensitive fields like password hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String phone;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
}
