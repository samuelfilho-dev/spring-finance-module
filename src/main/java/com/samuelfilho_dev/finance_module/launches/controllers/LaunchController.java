package com.samuelfilho_dev.finance_module.launches.controllers;

import com.samuelfilho_dev.finance_module.launches.dtos.CreateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.dtos.UpdateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.services.LaunchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "api/{version}/launches", version = "1")
public class LaunchController {
    private final LaunchService launchService;

    @GetMapping
    public ResponseEntity<List<LaunchResponse>> listLaunches() {
        var launches = this.launchService.findAllLaunches();
        return ResponseEntity.ok(launches);
    }

    @GetMapping("{id}")
    public ResponseEntity<LaunchResponse> findLaunchById(@PathVariable String id) {
        var launch = this.launchService.findLaunchById(id);
        return ResponseEntity.ok(launch);
    }

    @PostMapping
    public ResponseEntity<LaunchResponse> createLaunch(@Valid @RequestBody CreateLaunchRequest payload) {
        var launch = this.launchService.createLaunch(payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(launch);
    }

    @PutMapping("{id}")
    public ResponseEntity<LaunchResponse> updateLaunch(@PathVariable String id,
                                                       @Valid @RequestBody UpdateLaunchRequest payload) {
        var launch = this.launchService.updateLaunch(id, payload);
        return ResponseEntity.ok(launch);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<LaunchResponse> deleteLaunch(@PathVariable String id) {
        this.launchService.deleteLaunch(id);
        return ResponseEntity.noContent().build();
    }
}
