package com.samuelfilho_dev.finance_module.users.mappers;

import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import com.samuelfilho_dev.finance_module.users.entities.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}
