package com.quolance.quolance_api.unit.controllers;

import com.quolance.quolance_api.controllers.AuthController;
import com.quolance.quolance_api.dtos.LoginRequestDto;
import com.quolance.quolance_api.dtos.UserResponseDto;
import com.quolance.quolance_api.entities.User;
import com.quolance.quolance_api.entities.enums.Role;
import com.quolance.quolance_api.services.auth.AuthService;
import com.quolance.quolance_api.util.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    private LoginRequestDto loginRequest;
    private UserResponseDto userResponse;
    private User mockUser;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDto();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        mockUser.setRole(Role.CLIENT);
        mockUser.setVerified(true);
        mockUser.setProfileImageUrl(null);
        mockUser.setConnectedAccounts(new ArrayList<>());

        userResponse = new UserResponseDto(mockUser);
    }

    @Test
    void login_ReturnsOkResponse() {
        doNothing().when(authService).login(request, response, loginRequest);

        ResponseEntity<?> responseEntity = authController.login(request, response, loginRequest);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService, times(1)).login(request, response, loginRequest);
    }

    @Test
    void login_WhenInvalidCredentials_ThrowsBadCredentialsException() {
        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authService).login(request, response, loginRequest);

        assertThatThrownBy(() -> authController.login(request, response, loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(authService, times(1)).login(request, response, loginRequest);
    }

    @Test
    void login_WhenUserNotVerified_ThrowsApiException() {
        doThrow(new ApiException("User not verified"))
                .when(authService).login(request, response, loginRequest);

        assertThatThrownBy(() -> authController.login(request, response, loginRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage("User not verified");
        verify(authService, times(1)).login(request, response, loginRequest);
    }

    @Test
    void getSession_ReturnsUserResponse() {
        when(authService.getSession(request)).thenReturn(userResponse);

        ResponseEntity<UserResponseDto> responseEntity = authController.getSession(request);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo(userResponse);
        assertThat(responseEntity.getBody().getId()).isEqualTo(1L);
        assertThat(responseEntity.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(responseEntity.getBody().getFirstName()).isEqualTo("Test");
        assertThat(responseEntity.getBody().getLastName()).isEqualTo("User");
        assertThat(responseEntity.getBody().getRole()).isEqualTo(Role.CLIENT);
        verify(authService, times(1)).getSession(request);
    }

    @Test
    void getSession_WhenNoActiveSession_ThrowsApiException() {
        when(authService.getSession(request))
                .thenThrow(new ApiException("No active session found"));

        assertThatThrownBy(() -> authController.getSession(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("No active session found");
        verify(authService, times(1)).getSession(request);
    }

    @Test
    void logout_ReturnsOkResponse() {
        doNothing().when(authService).logout(request, response);

        ResponseEntity<Void> responseEntity = authController.logout(request, response);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService, times(1)).logout(request, response);
    }

    @Test
    void logout_WhenNoActiveSession_ThrowsApiException() {
        doThrow(new ApiException("No active session to logout from"))
                .when(authService).logout(request, response);

        assertThatThrownBy(() -> authController.logout(request, response))
                .isInstanceOf(ApiException.class)
                .hasMessage("No active session to logout from");
        verify(authService, times(1)).logout(request, response);
    }

    @Test
    void csrf_ReturnsOkResponse() {
        ResponseEntity<?> responseEntity = authController.csrf();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}