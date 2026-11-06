package com.foodconnect.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.foodconnect.entity.User;
import com.foodconnect.entity.enums.AccountStatus;
import com.foodconnect.entity.enums.Role;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "test-jwt-secret-key-for-testing-purposes-must-be-long-enough", 86400000L);
    }

    @Test
    void generateAndValidateToken() {
        User user = User.builder().id(1L).name("Test").email("test@test.com")
                .role(Role.RESTAURANT).accountStatus(AccountStatus.ACTIVE).build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        String token = tokenProvider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(1L);
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("test@test.com");
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(tokenProvider.validateToken("invalid.jwt.token")).isFalse();
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    void customUserDetails_activeUser_isEnabled() {
        User user = User.builder().id(1L).name("Test").email("test@test.com")
                .passwordHash("hash").role(Role.NGO).accountStatus(AccountStatus.ACTIVE).build();
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getUsername()).isEqualTo("test@test.com");
        assertThat(details.getRole()).isEqualTo(Role.NGO);
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_NGO");
    }

    @Test
    void customUserDetails_suspendedUser_isDisabled() {
        User user = User.builder().id(2L).name("Suspended").email("sus@test.com")
                .passwordHash("hash").role(Role.RESTAURANT).accountStatus(AccountStatus.SUSPENDED).build();
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isEnabled()).isFalse();
    }
}
