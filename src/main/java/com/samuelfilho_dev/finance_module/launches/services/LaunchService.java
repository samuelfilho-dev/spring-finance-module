package com.samuelfilho_dev.finance_module.launches.services;

import com.samuelfilho_dev.finance_module.launches.dtos.CreateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.dtos.UpdateLaunchRequest;

import java.util.List;

public interface LaunchService {
    List<LaunchResponse> findAllLaunches();

    LaunchResponse findLaunchById(String id);

    LaunchResponse createLaunch(CreateLaunchRequest payload);

    LaunchResponse updateLaunch(String id, UpdateLaunchRequest payload);

    void deleteLaunch(String id);
}
