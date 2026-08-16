package com.samuelfilho_dev.finance_module.launches.ofx;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfxBalance(
        BigDecimal amount,
        LocalDate asOf
) {
}
