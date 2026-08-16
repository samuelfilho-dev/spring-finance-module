package com.samuelfilho_dev.finance_module.launches.ofx;

import com.samuelfilho_dev.finance_module.launches.enums.AccountOfxType;

public record OfxAccount(
        AccountOfxType type,
        String bankId,
        String accountId,
        String accountType
) {
}
