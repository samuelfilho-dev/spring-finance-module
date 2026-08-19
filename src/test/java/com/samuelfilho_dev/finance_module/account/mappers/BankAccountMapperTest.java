package com.samuelfilho_dev.finance_module.account.mappers;

import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BankAccountMapperTest {

    private final BankAccountMapper mapper = Mappers.getMapper(BankAccountMapper.class);

    @Test
    void toResponse_shouldMapAccountFields() {
        var account = sampleAccount("665f1c2e8f1a2b3c4d5e6f7a");

        var response = mapper.toResponse(account);

        assertEquals(account.getId(), response.id());
        assertEquals(account.getBankName(), response.bankName());
        assertEquals(account.getAgency(), response.agency());
        assertEquals(account.getAccountNumber(), response.accountNumber());
        assertEquals("150.25", response.balance());
    }

    @Test
    void toResponse_shouldReturnNullWhenAccountIsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toResponseList_shouldMapEachAccount() {
        var first = sampleAccount("665f1c2e8f1a2b3c4d5e6f7a");
        var second = sampleAccount("665f1c2e8f1a2b3c4d5e6f7b");

        var responses = mapper.toResponseList(List.of(first, second));

        assertEquals(2, responses.size());
        assertEquals(first.getId(), responses.get(0).id());
        assertEquals(second.getId(), responses.get(1).id());
    }

    @Test
    void toResponseList_shouldReturnNullWhenListIsNull() {
        assertNull(mapper.toResponseList(null));
    }

    private static BankAccount sampleAccount(String id) {
        return BankAccount.builder()
                .id(id)
                .bankName("BANCO_NUBANK")
                .agency("0001")
                .accountNumber("12345-6")
                .balance(new BigDecimal("150.25"))
                .status(BankAccountStatus.ACTIVE)
                .userId(new ObjectId())
                .build();
    }
}
