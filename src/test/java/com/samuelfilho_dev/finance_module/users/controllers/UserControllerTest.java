package com.samuelfilho_dev.finance_module.users.controllers;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthPreTokenResponse;
import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UpdateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import com.samuelfilho_dev.finance_module.users.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String USER_ID = "665f1c2e8f1a2b3c4d5e6f7a";

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getAllUsers_shouldReturnOk() {
        var users = List.of(sampleUser());
        when(userService.findAllUsers()).thenReturn(users);

        var response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
    }

    @Test
    void getUserById_shouldReturnOk() {
        when(userService.findUserById(USER_ID)).thenReturn(sampleUser());

        var response = userController.getUserById(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleUser(), response.getBody());
    }

    @Test
    void createUser_shouldReturnCreated() {
        var payload = new CreateUserRequest("Samuel", "samuel@test.com", "secret", null);
        var body = new AuthPreTokenResponse(true, "ok", "/path", "token", "qr", "otp");
        when(userService.createUser(payload)).thenReturn(body);

        var response = userController.createUser(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(body, response.getBody());
    }

    @Test
    void createAdmin_shouldReturnCreated() {
        var payload = new CreateUserRequest("Admin", "admin@test.com", "secret", null);
        when(userService.createAdminUser(payload)).thenReturn(sampleUser());

        var response = userController.createAdmin(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleUser(), response.getBody());
    }

    @Test
    void updateUser_shouldReturnOk() {
        var payload = new UpdateUserRequest("Samuel", "samuel@test.com", null);
        when(userService.updateUserById(USER_ID, payload)).thenReturn(sampleUser());

        var response = userController.updateUser(USER_ID, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleUser(), response.getBody());
    }

    @Test
    void deleteUserById_shouldReturnNoContent() {
        var response = userController.deleteUserById(USER_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService).deleteUserById(USER_ID);
    }

    private static UserResponse sampleUser() {
        return new UserResponse(USER_ID, "Samuel", "samuel@test.com", null, null);
    }
}
