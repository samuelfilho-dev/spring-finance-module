package com.samuelfilho_dev.finance_module.launches.utils;

import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaunchUtilsTest {

    @Test
    void calculateNewBalance_shouldAddRecipesAndSubtractExpenses() {
        assertEquals(new BigDecimal("150"), LaunchUtils.calculateNewBalance(LaunchType.RECIPE, new BigDecimal("50"), new BigDecimal("100")));
        assertEquals(new BigDecimal("70"), LaunchUtils.calculateNewBalance(LaunchType.EXPENSE, new BigDecimal("30"), new BigDecimal("100")));
    }

    @Test
    void reverseBalance_shouldUndoRecipeAndExpense() {
        assertEquals(new BigDecimal("100"), LaunchUtils.reverseBalance(LaunchType.RECIPE, new BigDecimal("50"), new BigDecimal("150")));
        assertEquals(new BigDecimal("100"), LaunchUtils.reverseBalance(LaunchType.EXPENSE, new BigDecimal("30"), new BigDecimal("70")));
    }

    @Test
    void calculateNewBalanceWithLaunches_shouldReturnOriginalBalanceWhenListIsEmpty() {
        assertEquals(new BigDecimal("100"), LaunchUtils.calculateNewBalanceWithLaunches(List.of(), new BigDecimal("100")));
    }

    @Test
    void calculateNewBalanceWithLaunches_shouldApplyLastMappedBalance() {
        var recipe = Launch.builder().type(LaunchType.RECIPE).amount(new BigDecimal("10")).build();
        var expense = Launch.builder().type(LaunchType.EXPENSE).amount(new BigDecimal("3")).build();

        var result = LaunchUtils.calculateNewBalanceWithLaunches(List.of(recipe, expense), new BigDecimal("100"));

        assertEquals(new BigDecimal("97"), result);
    }
}
