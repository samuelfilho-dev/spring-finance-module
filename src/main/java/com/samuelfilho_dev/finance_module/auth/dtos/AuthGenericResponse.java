package com.samuelfilho_dev.finance_module.auth.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthGenericResponse(
        Boolean success,
        String message,
        String path,
        String token
) {
}
