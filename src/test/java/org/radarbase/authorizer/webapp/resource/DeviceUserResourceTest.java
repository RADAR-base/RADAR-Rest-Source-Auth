package org.radarbase.authorizer.webapp.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.radarbase.authorizer.RadarDeviceAuthorizerApplication;
import org.radarbase.authorizer.domain.DeviceUser;
import org.radarbase.authorizer.repository.DeviceUserRepository;
import org.radarbase.authorizer.service.DeviceUserService;
import org.radarbase.authorizer.service.dto.DeviceUserPropertiesDTO;
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
public class DeviceUserResourceTest {

    @Autowired
    private DeviceUserService deviceUserService;

    @Autowired
    private DeviceUserRepository deviceUserRepository;

    public static final String DEFAULT_PROJ_NAME = "Test-proj";
    public static final String DEFAULT_USER_ID = "Test-sub";
    public static final String DEFAULT_SOURCE_ID = "Test-source";
    public static final String DEFAULT_DEVICE_TYPE = "Fitbit";
    public static final Instant DEFAULT_START_TIME = Instant.now().minus(Duration.ofHours(1));
    public static final Instant DEFAULT_END_TIME = Instant.now().plus(Duration.ofHours(1));
    public static final Boolean DEFAULT_AUTHORIZED = false;
    public static final String DEFAULT_EXTERNAL_USER_ID = "86420984";

    private DeviceUser sampleDeviceUser;

    private MockMvc restUserMockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        DeviceUserResource deviceUserResource = new DeviceUserResource();
        ReflectionTestUtils.setField(deviceUserResource, "deviceService", deviceUserService);



        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(deviceUserResource).build();
    }


    /**
     * Create an entity for this test.
     *
     * <p>This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.</p>
     */
    public static DeviceUser createEntity() {
        return new DeviceUser()
                .projectId(DEFAULT_PROJ_NAME)
                .userId(DEFAULT_USER_ID)
                .sourceId(DEFAULT_SOURCE_ID)
                .startDate(DEFAULT_START_TIME)
                .endDate(DEFAULT_END_TIME)
                .externalUserId(DEFAULT_EXTERNAL_USER_ID)
                .authorized(DEFAULT_AUTHORIZED);
    }

    public static DeviceUserPropertiesDTO createDefaultDeviceDto() {
        return new DeviceUserPropertiesDTO(createEntity());
    }

    @Before
    public void initTest() {
        sampleDeviceUser = createEntity();
    }

    @Test
    @Transactional
    public void getAllUsers() throws Exception {
        // Initialize the database
        deviceUserRepository.saveAndFlush(sampleDeviceUser);

        // Get all the users
        restUserMockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].projectId").value(hasItem(DEFAULT_PROJ_NAME)))
                .andExpect(jsonPath("$.[*].userId").value(hasItem(DEFAULT_USER_ID)))
                .andExpect(jsonPath("$.[*].sourceId").value(hasItem(DEFAULT_SOURCE_ID)))
                .andExpect(jsonPath("$.[*].externalUserId").value(
                        hasItem(DEFAULT_EXTERNAL_USER_ID.toString())));
    }


}
