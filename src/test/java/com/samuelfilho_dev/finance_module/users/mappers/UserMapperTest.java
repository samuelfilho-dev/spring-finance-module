package com.samuelfilho_dev.finance_module.users.mappers;

import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import com.samuelfilho_dev.finance_module.users.entities.Address;
import com.samuelfilho_dev.finance_module.users.entities.User;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toResponse_shouldMapUserAddressAndAccounts() {
        var userId = new ObjectId();
        var address = Address.builder()
                .id("addr-1")
                .street("Rua A")
                .number("10")
                .city("São Paulo")
                .state("SP")
                .postalCode("01310100")
                .userId(userId)
                .build();
        var account = BankAccount.builder()
                .id(new ObjectId().toHexString())
                .bankName("BANCO_NUBANK")
                .agency("0001")
                .accountNumber("123")
                .balance(new BigDecimal("10.50"))
                .status(BankAccountStatus.ACTIVE)
                .userId(userId)
                .build();
        var user = User.builder()
                .id(userId.toHexString())
                .name("Samuel")
                .email("samuel@test.com")
                .address(address)
                .accounts(List.of(account))
                .build();

        var response = mapper.toResponse(user);

        assertEquals(user.getId(), response.id());
        assertEquals("Samuel", response.name());
        assertEquals("samuel@test.com", response.email());
        assertEquals("addr-1", response.address().id());
        assertEquals("Rua A", response.address().street());
        assertEquals(1, response.accounts().size());
        assertEquals("BANCO_NUBANK", response.accounts().get(0).bankName());
        assertEquals("10.50", response.accounts().get(0).balance());
    }

    @Test
    void toResponse_shouldReturnNullWhenUserIsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toResponseList_shouldMapCollection() {
        var user = User.builder().id("1").name("A").email("a@test.com").build();

        var responses = mapper.toResponseList(List.of(user));

        assertEquals(1, responses.size());
        assertEquals("A", responses.get(0).name());
    }

    @Test
    void toResponse_shouldMapUserWithoutAddressOrAccounts() {
        var user = User.builder().id("1").name("A").email("a@test.com").build();

        var response = mapper.toResponse(user);

        assertNull(response.address());
        assertNull(response.accounts());
    }

    @Test
    void toResponseList_shouldReturnNullWhenListIsNull() {
        assertNull(mapper.toResponseList(null));
    }
}
