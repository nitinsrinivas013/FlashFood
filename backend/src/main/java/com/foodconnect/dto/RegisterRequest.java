package com.foodconnect.dto;

import com.foodconnect.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 * Contains common fields plus role-specific fields for Restaurant or NGO profiles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    private String phone;

    // Restaurant-specific fields
    private String restaurantName;
    private String address;
    private Double latitude;
    private Double longitude;

    // NGO-specific fields
    private String ngoName;
    private String registrationNumber;
}
