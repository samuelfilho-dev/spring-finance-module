package com.samuelfilho_dev.finance_module.launches.ofx;

import java.time.LocalDate;
import java.util.List;

public record OfxStatement(
        OfxAccount account,
        String currency,
        LocalDate periodStart,
        LocalDate periodEnd,
        List<OfxTransaction> transactions,
        OfxBalance ledgerBalance,
        OfxBalance availableBalance
) {
}
