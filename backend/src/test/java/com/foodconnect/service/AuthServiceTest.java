package com.foodconnect.service;

import com.foodconnect.dto.AuthResponse;
import com.foodconnect.dto.LoginRequest;
import com.foodconnect.dto.RegisterRequest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.foodconnect.entity.User;
import com.foodconnect.entity.enums.Role;
import com.foodconnect.exception.DuplicateResourceException;
import com.foodconnect.mapper.UserMapper;
import com.foodconnect.repository.NgoProfileRepository;
import com.foodconnect.repository.RestaurantProfileRepository;
import com.foodconnect.repository.UserRepository;
import com.foodconnect.security.CustomUserDetails;
import com.foodconnect.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RestaurantProfileRepository restaurantProfileRepository;
    @Mock private NgoProfileRepository ngoProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest restaurantRegRequest;
    private RegisterRequest ngoRegRequest;

    @BeforeEach
    void setUp() {
        restaurantRegRequest = RegisterRequest.builder()
                .name("Test Restaurant Owner")
                .email("restaurant@test.com")
                .password("password123")
                .role(Role.RESTAURANT)
                .phone("1234567890")
                .restaurantName("Test Restaurant")
                .address("123 Test St")
                .latitude(28.6139)
                .longitude(77.2090)
                .build();

        ngoRegRequest = RegisterRequest.builder()
                .name("Test NGO Contact")
                .email("ngo@test.com")
                .password("password123")
                .role(Role.NGO)
                .ngoName("Test NGO")
                .address("456 Help St")
                .latitude(28.6200)
                .longitude(77.2100)
                .build();
    }

    @Test
    void register_restaurant_success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedpw");
        User savedUser = User.builder().id(1L).name("Test").email("restaurant@test.com")
                .role(Role.RESTAURANT).build();
        when(userRepository.save(any())).thenReturn(savedUser);
        when(restaurantProfileRepository.save(any())).thenReturn(null);

        Authentication auth = mock(Authentication.class);
        CustomUserDetails ud = mock(CustomUserDetails.class);
        when(ud.getId()).thenReturn(1L);
        when(ud.getEmail()).thenReturn("restaurant@test.com");
        when(ud.getRole()).thenReturn(Role.RESTAURANT);
        when(auth.getPrincipal()).thenReturn(ud);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token");
        when(userMapper.toUserResponse(any())).thenReturn(null);

        AuthResponse response = authService.register(restaurantRegRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any());
        verify(restaurantProfileRepository).save(any());
        verify(ngoProfileRepository, never()).save(any());
    }

    @Test
    void register_ngo_success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedpw");
        User savedUser = User.builder().id(2L).name("NGO").email("ngo@test.com")
                .role(Role.NGO).build();
        when(userRepository.save(any())).thenReturn(savedUser);
        when(ngoProfileRepository.save(any())).thenReturn(null);

        Authentication auth = mock(Authentication.class);
        CustomUserDetails ud = mock(CustomUserDetails.class);
        when(ud.getId()).thenReturn(2L);
        when(ud.getEmail()).thenReturn("ngo@test.com");
        when(ud.getRole()).thenReturn(Role.NGO);
        when(auth.getPrincipal()).thenReturn(ud);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token-ngo");
        when(userMapper.toUserResponse(any())).thenReturn(null);

        AuthResponse response = authService.register(ngoRegRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token-ngo");
        verify(ngoProfileRepository).save(any());
        verify(restaurantProfileRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("restaurant@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(restaurantRegRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        Authentication auth = mock(Authentication.class);
        CustomUserDetails ud = mock(CustomUserDetails.class);
        when(ud.getId()).thenReturn(1L);
        when(auth.getPrincipal()).thenReturn(ud);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.generateTokenForUser(any())).thenReturn("login-token");

        User user = User.builder().id(1L).name("Test").email("test@test.com").role(Role.RESTAURANT).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(any())).thenReturn(null);

        LoginRequest loginReq = LoginRequest.builder()
                .email("test@test.com").password("password123").build();

        AuthResponse response = authService.login(loginReq);

        assertThat(response.getToken()).isEqualTo("login-token");
        assertThat(response.getRole()).isEqualTo(Role.RESTAURANT);
    }
}
