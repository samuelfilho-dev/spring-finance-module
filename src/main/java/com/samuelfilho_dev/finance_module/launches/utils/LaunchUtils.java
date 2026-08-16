package com.samuelfilho_dev.finance_module.launches.utils;

import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;

import java.math.BigDecimal;
import java.util.List;

public class LaunchUtils {

    public static BigDecimal calculateNewBalance(LaunchType type, BigDecimal amount, BigDecimal balance) {
        return switch (type) {
            case RECIPE -> balance.add(amount);
            case EXPENSE -> balance.subtract(amount);
        };
    }

    public static BigDecimal reverseBalance(LaunchType type, BigDecimal amount, BigDecimal balance) {
        return switch (type) {
            case RECIPE -> balance.subtract(amount);
            case EXPENSE -> balance.add(amount);
        };
    }

    public static BigDecimal calculateNewBalanceWithLaunches(List<Launch> launches, BigDecimal balance) {
        return launches.stream()
                .map(launch -> calculateNewBalance(launch.getType(), launch.getAmount(), balance))
                .reduce(balance, (a, b) -> b);
    }
}
