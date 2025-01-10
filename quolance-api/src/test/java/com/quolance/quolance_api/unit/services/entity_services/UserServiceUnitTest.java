package com.quolance.quolance_api.services.entity_services.impl;

import com.quolance.quolance_api.dtos.*;
import com.quolance.quolance_api.entities.PasswordResetToken;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.VerificationCode;
import com.quolance.quolance_api.entities.enums.Role;
import com.quolance.quolance_api.jobs.SendResetPasswordEmailJob;
import com.quolance.quolance_api.jobs.SendWelcomeEmailJob;
import com.quolance.quolance_api.repositories.PasswordResetTokenRepository;
import com.quolance.quolance_api.repositories.UserRepository;
import com.quolance.quolance_api.repositories.VerificationCodeRepository;
import com.quolance.quolance_api.util.exceptions.ApiException;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<VerificationCode> verificationCodeCaptor;

    @Captor
    private ArgumentCaptor<PasswordResetToken> passwordResetTokenCaptor;

    private User mockUser;
    private VerificationCode mockVerificationCode;
    private PasswordResetToken mockPasswordResetToken;
    private CreateUserRequestDto createUserRequest;
    private CreateAdminRequestDto createAdminRequest;
    private UpdateUserRequestDto updateUserRequest;
    private UpdateUserPasswordRequestDto updatePasswordRequest;
    private UpdatePendingUserRequestDto updatePendingUserRequest;

    private User createMockUser() {
        return User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encoded_password")
                .firstName("Test")
                .lastName("User")
                .role(Role.CLIENT)
                .verified(false)
                .connectedAccounts(new ArrayList<>())
                .build();
    }

    @BeforeEach
    void setUp() {
        mockUser = createMockUser();

        mockVerificationCode = new VerificationCode(mockUser);
        mockVerificationCode.setCode("test_code");

        mockPasswordResetToken = new PasswordResetToken(mockUser);
        mockPasswordResetToken.onEmailSent();

        createUserRequest = CreateUserRequestDto.builder()
                .email("test@test.com")
                .password("Password123")
                .passwordConfirmation("Password123")
                .firstName("Test")
                .lastName("User")
                .role(Role.CLIENT.name())
                .build();

        createAdminRequest = CreateAdminRequestDto.builder()
                .email("admin@test.com")
                .temporaryPassword("Admin123")
                .passwordConfirmation("Admin123")
                .firstName("Admin")
                .lastName("User")
                .build();

        updateUserRequest = UpdateUserRequestDto.builder()
                .firstName("Updated")
                .lastName("User")
                .build();

        updatePasswordRequest = UpdateUserPasswordRequestDto.builder()
                .oldPassword("OldPassword123")
                .password("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        updatePendingUserRequest = new UpdatePendingUserRequestDto();
        updatePendingUserRequest.setPassword("NewPassword123");
        updatePendingUserRequest.setConfirmPassword("NewPassword123");
        updatePendingUserRequest.setRole(Role.FREELANCER.name());
    }

    @Test
    void create_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponseDto result = userService.create(createUserRequest);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo(createUserRequest.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(createUserRequest.getLastName());
        assertThat(savedUser.getRole()).isEqualTo(Role.valueOf(createUserRequest.getRole()));
        verify(verificationCodeRepository).save(any(VerificationCode.class));
    }

    @Test
    void create_EmailExists_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(createUserRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage("A user with this email already exists.");
    }

    @Test
    void createAdmin_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponseDto result = userService.createAdmin(createAdminRequest);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo(createAdminRequest.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(createAdminRequest.getLastName());
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void verifyEmail_Success() {
        when(verificationCodeRepository.findByCode("test_code")).thenReturn(Optional.of(mockVerificationCode));

        userService.verifyEmail("test_code");

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.isVerified()).isTrue();
        verify(verificationCodeRepository).delete(mockVerificationCode);
    }

    @Test
    void verifyEmail_InvalidCode_ThrowsException() {
        when(verificationCodeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyEmail("invalid_code"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 400)
                .hasMessage("Invalid token");
    }

    @Test
    void forgotPassword_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(mockPasswordResetToken);

        userService.forgotPassword("test@test.com");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.forgotPassword("nonexistent@test.com"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 404)
                .hasMessage("User not found");
    }

    @Test
    void resetPassword_Success() {
        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.of(mockPasswordResetToken));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        updatePasswordRequest.setPasswordResetToken("reset_token");
        userService.resetPassword(updatePasswordRequest);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void resetPassword_TokenNotFound_ThrowsException() {
        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        updatePasswordRequest.setPasswordResetToken("invalid_token");
        assertThatThrownBy(() -> userService.resetPassword(updatePasswordRequest))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 404)
                .hasMessage("Password reset token not found");
    }

    @Test
    void resetPassword_TokenExpired_ThrowsException() {
        try {
            java.lang.reflect.Field expiresAtField = PasswordResetToken.class.getDeclaredField("expiresAt");
            expiresAtField.setAccessible(true);
            expiresAtField.set(mockPasswordResetToken, LocalDateTime.now().minusHours(1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(passwordResetTokenRepository.findByToken(anyString())).thenReturn(Optional.of(mockPasswordResetToken));

        updatePasswordRequest.setPasswordResetToken(mockPasswordResetToken.getToken());
        assertThatThrownBy(() -> userService.resetPassword(updatePasswordRequest))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 400)
                .hasMessage("Password reset token is expired");
    }

    @Test
    void updateUserInfoUser_Success() {
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponseDto result = userService.updateUser(updateUserRequest, mockUser);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFirstName()).isEqualTo(updateUserRequest.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(updateUserRequest.getLastName());
    }

    @Test
    void updateUserInfoPassword_Success() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponseDto result = userService.updatePassword(updatePasswordRequest, mockUser);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserInfoPassword_WrongOldPassword_ThrowsException() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(updatePasswordRequest, mockUser))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", 400)
                .hasMessage("Wrong password");
    }
}