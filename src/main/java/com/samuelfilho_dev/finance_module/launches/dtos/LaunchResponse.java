package com.samuelfilho_dev.finance_module.launches.dtos;

import com.samuelfilho_dev.finance_module.launches.enums.LaunchCategory;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;

import java.math.BigDecimal;
import java.time.Instant;

public record LaunchResponse(
        String id,
        String title,
        String description,
        Instant launchDate,
        BigDecimal amount,
        LaunchType type,
        LaunchCategory category
) {
}
