package org.radarbase.authorizer.webapp.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.radarbase.authorizer.RadarDeviceAuthorizerApplication;
import org.radarbase.authorizer.RadarDeviceAuthorizerApplicationTests;
import org.radarbase.authorizer.domain.Device;
import org.radarbase.authorizer.repository.DeviceRepository;
import org.radarbase.authorizer.service.DeviceService;
import org.radarbase.authorizer.service.dto.DevicePropertiesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RadarDeviceAuthorizerApplication.class)
public class DeviceResourceTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    private static final String DEFAULT_PROJ_NAME = "Test-proj";
    private static final String DEFAULT_USER_ID = "Test-sub";
    private static final String DEFAULT_SOURCE_ID = "Test-source";
    private static final String DEFAULT_DEVICE_TYPE = "Fitbit";
    private static final Instant DEFAULT_START_TIME = Instant.now().minus(Duration.ofHours(1));
    private static final Instant DEFAULT_END_TIME = Instant.now().plus(Duration.ofHours(1));
    private static final Boolean DEFAULT_AUTHORIZED = false;
    private static final String DEFAULT_EXTERNAL_USER_ID = "86420984";


    private Device sampleDevice;

    private MockMvc restUserMockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        DeviceResource deviceResource = new DeviceResource();
        ReflectionTestUtils.setField(deviceResource, "deviceService", deviceService);



        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(deviceResource).build();
    }

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

    @Before
    public void initTest() {
        sampleDevice = createEntity();
    }

    @Test
    @Transactional
    public void createDevice() throws Exception {
        final int databaseSizeBeforeCreate = deviceRepository.findAll().size();

        DevicePropertiesDTO devicePropertiesDTO = createDefaultDevice();

        restUserMockMvc.perform(post("/devices")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(devicePropertiesDTO)))
                .andExpect(status().isCreated());

        // Validate the User in the database
        List<Device> userList = deviceRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate + 1);
        Device createdDevice = userList.get(userList.size() - 1);

    }

    @Test
    @Transactional
    public void getAllSourceTypes() throws Exception {
        // Initialize the database
        deviceRepository.saveAndFlush(sampleDevice);

        // Get all the sourceTypeList
        restUserMockMvc.perform(get("/devices"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].projectId").value(hasItem(DEFAULT_PROJ_NAME)))
                .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID)))
                .andExpect(jsonPath("$.[*].sourceId").value(hasItem(DEFAULT_SOURCE_ID)))
                .andExpect(jsonPath("$.[*].externalUserId").value(
                        hasItem(DEFAULT_EXTERNAL_USER_ID.toString())));
    }

    private DevicePropertiesDTO createDefaultDevice() {
        DevicePropertiesDTO result = new DevicePropertiesDTO();
        result.projectId("test-proj");
        result.userId("userId");
        result.sourceId("sepid");
        result.stateDate(Instant.now().minus(Duration.ofHours(1)));
        result.endDate(Instant.now().plus(Duration.ofHours(1)));
        result.authorized(false);
        result.externalDeviceId("rspthinr");
        return result;
    }
}
