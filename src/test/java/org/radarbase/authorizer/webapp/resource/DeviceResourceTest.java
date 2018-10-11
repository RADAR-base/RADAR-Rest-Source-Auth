package org.radarbase.authorizer.webapp.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.radarbase.authorizer.service.DeviceServiceTest.DEFAULT_EXTERNAL_USER_ID;
import static org.radarbase.authorizer.service.DeviceServiceTest.DEFAULT_PROJ_NAME;
import static org.radarbase.authorizer.service.DeviceServiceTest.DEFAULT_SOURCE_ID;
import static org.radarbase.authorizer.service.DeviceServiceTest.DEFAULT_USER_ID;
import static org.radarbase.authorizer.service.DeviceServiceTest.createDefaultDeviceDto;
import static org.radarbase.authorizer.service.DeviceServiceTest.createEntity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.radarbase.authorizer.RadarDeviceAuthorizerApplication;
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

    private Device sampleDevice;

    private MockMvc restUserMockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        DeviceResource deviceResource = new DeviceResource();
        ReflectionTestUtils.setField(deviceResource, "deviceService", deviceService);



        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(deviceResource).build();
    }



    @Before
    public void initTest() {
        sampleDevice = createEntity();
    }

    @Test
    @Transactional
    public void createDevice() throws Exception {
        final int databaseSizeBeforeCreate = deviceRepository.findAll().size();

        DevicePropertiesDTO devicePropertiesDTO = createDefaultDeviceDto();

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


}
