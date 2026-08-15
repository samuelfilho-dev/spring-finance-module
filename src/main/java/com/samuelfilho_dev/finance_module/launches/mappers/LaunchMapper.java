package com.samuelfilho_dev.finance_module.launches.mappers;

import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LaunchMapper {
    LaunchResponse toResponse(Launch launch);

    List<LaunchResponse> toResponseList(List<Launch> launches);
}
