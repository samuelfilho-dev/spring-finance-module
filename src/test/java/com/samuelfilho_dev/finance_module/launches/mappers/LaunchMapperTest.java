package com.samuelfilho_dev.finance_module.launches.mappers;

import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchCategory;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LaunchMapperTest {

    private final LaunchMapper mapper = Mappers.getMapper(LaunchMapper.class);

    @Test
    void toResponse_shouldMapLaunchFields() {
        var launch = Launch.builder()
                .id(new ObjectId().toHexString())
                .title("Salary")
                .description("desc")
                .launchDate(Instant.parse("2026-01-01T00:00:00Z"))
                .amount(new BigDecimal("100.00"))
                .type(LaunchType.RECIPE)
                .category(LaunchCategory.SALARY)
                .userId(new ObjectId())
                .bankAccountId(new ObjectId())
                .build();

        var response = mapper.toResponse(launch);

        assertEquals(launch.getId(), response.id());
        assertEquals("Salary", response.title());
        assertEquals("desc", response.description());
        assertEquals(launch.getLaunchDate(), response.launchDate());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals(LaunchType.RECIPE, response.type());
        assertEquals(LaunchCategory.SALARY, response.category());
    }

    @Test
    void toResponse_shouldReturnNullWhenLaunchIsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toResponseList_shouldMapCollection() {
        var launch = Launch.builder().id("1").title("A").amount(BigDecimal.ONE).type(LaunchType.EXPENSE).category(LaunchCategory.FOOD).build();
        var responses = mapper.toResponseList(List.of(launch));
        assertEquals(1, responses.size());
        assertEquals("A", responses.get(0).title());
    }

    @Test
    void toResponseList_shouldReturnNullWhenListIsNull() {
        assertNull(mapper.toResponseList(null));
    }
}
