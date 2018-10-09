package org.radarbase.authorizer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.radarbase.authorizer.RadarDeviceAuthorizerApplication;
import org.radarbase.authorizer.domain.Device;
import org.radarbase.authorizer.service.dto.DevicePropertiesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RadarDeviceAuthorizerApplication.class)
@Transactional
public class DeviceServiceTest {

    public static final String DEFAULT_PROJ_NAME = "Test-proj";
    public static final String DEFAULT_USER_ID = "Test-sub";
    public static final String DEFAULT_SOURCE_ID = "Test-source";
    public static final String DEFAULT_DEVICE_TYPE = "Fitbit";
    public static final Instant DEFAULT_START_TIME = Instant.now().minus(Duration.ofHours(1));
    public static final Instant DEFAULT_END_TIME = Instant.now().plus(Duration.ofHours(1));
    public static final Boolean DEFAULT_AUTHORIZED = false;
    public static final String DEFAULT_EXTERNAL_USER_ID = "86420984";

    @Autowired
    private DeviceService deviceService;

    /**
     * Create an entity for this test.
     *
     * <p>This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.</p>
     */
    public static Device createEntity() {
        return new Device()
                .projectId(DEFAULT_PROJ_NAME)
                .userId(DEFAULT_USER_ID)
                .sourceId(DEFAULT_SOURCE_ID)
                .startDate(DEFAULT_START_TIME)
                .endDate(DEFAULT_END_TIME)
                .externalUserId(DEFAULT_EXTERNAL_USER_ID)
                .authorized(DEFAULT_AUTHORIZED);
    }

    public static DevicePropertiesDTO createDefaultDeviceDto() {
        return new DevicePropertiesDTO(createEntity());
    }

    @Test
    @Transactional
    public void testGetDevices() {

        deviceService.save(createDefaultDeviceDto());


    }
}
