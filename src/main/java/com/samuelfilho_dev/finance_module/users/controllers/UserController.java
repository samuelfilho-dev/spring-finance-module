package com.samuelfilho_dev.finance_module.users.controllers;

import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import com.samuelfilho_dev.finance_module.users.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "api/{version}/users", version = "1")
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest payload) {
        return ResponseEntity.ok(userService.createUser(payload));
    }
}
