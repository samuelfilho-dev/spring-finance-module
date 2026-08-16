package com.samuelfilho_dev.finance_module.launches.ofx;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfxTransaction(
        String type,
        LocalDate datePosted,
        BigDecimal amount,
        String fitId,
        String checkNumber,
        String name,
        String memo
) {
}
