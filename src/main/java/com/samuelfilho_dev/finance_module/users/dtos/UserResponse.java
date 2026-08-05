package com.samuelfilho_dev.finance_module.users.dtos;

public record UserResponse(
        String id,
        String name,
        String email,
        AddressResponse address
) {

}
