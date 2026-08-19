package com.samuelfilho_dev.finance_module.users.services
;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthPreTokenResponse;
import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UpdateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;

import java.util.List;

public interface UserService {
    AuthPreTokenResponse createUser(CreateUserRequest payload);

    UserResponse createAdminUser(CreateUserRequest payload);

    List<UserResponse> findAllUsers();

    UserResponse findUserById(String id);

    UserResponse findUserByEmail(String email);

    UserResponse updateUserById(String id, UpdateUserRequest payload);

    void deleteUserById(String id);

}
