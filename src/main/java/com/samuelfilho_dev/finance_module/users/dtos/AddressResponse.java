package com.samuelfilho_dev.finance_module.users.dtos;

public record AddressResponse(
        String id,
        String street,
        String number,
        String complement,
        String city,
        String state,
        String postalCode
) {
}
