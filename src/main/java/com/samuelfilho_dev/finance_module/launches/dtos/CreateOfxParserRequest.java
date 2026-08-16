package com.samuelfilho_dev.finance_module.launches.dtos;

import org.springframework.web.multipart.MultipartFile;

public record CreateOfxParserRequest(
        String userId,
        String bankAccountId,
        MultipartFile file
) {
}
