package com.samuelfilho_dev.finance_module.users.services
;

import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import org.bson.Document;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest payload);

    List<UserResponse> findAllUsers();

    UserResponse findUserById(String id);

    UserResponse findUserByEmail(String email);

    UserResponse updateUserById(String id, UserResponse payload);

    void deleteUserById(String id);

}
