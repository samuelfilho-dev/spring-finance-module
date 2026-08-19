package com.samuelfilho_dev.finance_module.launches.controllers;

import com.samuelfilho_dev.finance_module.launches.dtos.CreateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.CreateOfxParserRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.dtos.OfxResponse;
import com.samuelfilho_dev.finance_module.launches.dtos.UpdateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchCategory;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import com.samuelfilho_dev.finance_module.launches.services.LaunchService;
import com.samuelfilho_dev.finance_module.launches.services.OfxParserService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchControllerTest {

    private static final String LAUNCH_ID = new ObjectId().toHexString();
    private static final String ACCOUNT_ID = new ObjectId().toHexString();

    @Mock
    private LaunchService launchService;
    @Mock
    private OfxParserService ofxParserService;

    @InjectMocks
    private LaunchController launchController;

    @Test
    void listLaunches_shouldReturnOk() {
        var launches = List.of(sampleLaunch());
        when(launchService.findAllLaunches()).thenReturn(launches);

        var response = launchController.listLaunches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(launches, response.getBody());
    }

    @Test
    void findLaunchById_shouldReturnOk() {
        when(launchService.findLaunchById(LAUNCH_ID)).thenReturn(sampleLaunch());

        var response = launchController.findLaunchById(LAUNCH_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleLaunch(), response.getBody());
    }

    @Test
    void createLaunch_shouldReturnCreated() {
        var payload = new CreateLaunchRequest("Salary", null, Instant.parse("2026-01-01T00:00:00Z"), BigDecimal.TEN, LaunchType.RECIPE, LaunchCategory.SALARY, ACCOUNT_ID);
        when(launchService.createLaunch(payload)).thenReturn(sampleLaunch());

        var response = launchController.createLaunch(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(sampleLaunch(), response.getBody());
    }

    @Test
    void updateLaunch_shouldReturnOk() {
        var payload = new UpdateLaunchRequest("Salary", null, Instant.parse("2026-01-01T00:00:00Z"), BigDecimal.TEN, LaunchType.RECIPE, LaunchCategory.SALARY);
        when(launchService.updateLaunch(LAUNCH_ID, payload)).thenReturn(sampleLaunch());

        var response = launchController.updateLaunch(LAUNCH_ID, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleLaunch(), response.getBody());
    }

    @Test
    void deleteLaunch_shouldReturnNoContent() {
        var response = launchController.deleteLaunch(LAUNCH_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(launchService).deleteLaunch(LAUNCH_ID);
    }

    @Test
    void createLaunchesByOfxFile_shouldReturnOk() {
        var payload = new CreateOfxParserRequest(ACCOUNT_ID, new MockMultipartFile("file", "a.ofx", "text/plain", new byte[]{1}));
        var body = new OfxResponse(List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO);
        when(ofxParserService.exec(payload)).thenReturn(body);

        var response = launchController.createLaunchesByOfxFile(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body, response.getBody());
    }

    private static LaunchResponse sampleLaunch() {
        return new LaunchResponse(LAUNCH_ID, "Salary", null, Instant.parse("2026-01-01T00:00:00Z"), BigDecimal.TEN, LaunchType.RECIPE, LaunchCategory.SALARY);
    }
}
