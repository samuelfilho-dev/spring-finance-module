package com.samuelfilho_dev.finance_module.launches.dtos;

import com.samuelfilho_dev.finance_module.validators.ObjectId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record CreateOfxParserRequest(

        @NotBlank(message = "ID da Conta Bancaria é requerida")
        @ObjectId
        String bankAccountId,

        @NotNull(message = "Arquivo OFX é requerido")
        MultipartFile file
) {
}
