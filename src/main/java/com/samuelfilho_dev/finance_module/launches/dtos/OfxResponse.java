package com.samuelfilho_dev.finance_module.launches.dtos;

import java.math.BigDecimal;
import java.util.List;

public record OfxResponse(
        List<LaunchResponse> launches,
        Integer totalStatements,
        BigDecimal oldBalance,
        BigDecimal newBalance
) {
}
