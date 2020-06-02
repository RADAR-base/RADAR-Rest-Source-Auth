/*
 *
 *  * Copyright 2018 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer.webapp.resource;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.radarbase.authorizer.RadarRestSourceAuthorizerApplication;
import org.radarbase.authorizer.domain.RestSourceUser;
import org.radarbase.authorizer.repository.RestSourceUserRepository;
import org.radarbase.authorizer.service.RestSourceUserService;
import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RadarRestSourceAuthorizerApplication.class)
public class RestSourceUserResourceTest {

    @Autowired
    private RestSourceUserService restSourceUserService;

    @Autowired
    private RestSourceUserRepository restSourceUserRepository;

    public static final String DEFAULT_PROJ_NAME = "Test-proj";
    public static final String DEFAULT_USER_ID = "Test-sub";
    public static final String DEFAULT_SOURCE_ID = "Test-source";
    public static final String DEFAULT_DEVICE_TYPE = "Fitbit";
    public static final Instant DEFAULT_START_TIME = Instant.now().minus(Duration.ofHours(1));
    public static final Instant DEFAULT_END_TIME = Instant.now().plus(Duration.ofHours(1));
    public static final Boolean DEFAULT_AUTHORIZED = false;
    public static final String DEFAULT_EXTERNAL_USER_ID = "86420984";

    private RestSourceUser sampleRestSourceUser;

    private MockMvc restUserMockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        RestSourceUserResource restSourceUserResource =
                new RestSourceUserResource(restSourceUserService, null);
        ReflectionTestUtils.setField(restSourceUserResource, "restSourceUserService", restSourceUserService);



        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(restSourceUserResource).build();
    }


    /**
     * Create an entity for this test.
     *
     * <p>This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.</p>
     */
    public static RestSourceUser createEntity() {
        return new RestSourceUser()
                .projectId(DEFAULT_PROJ_NAME)
                .userId(DEFAULT_USER_ID)
                .sourceId(DEFAULT_SOURCE_ID)
                .startDate(DEFAULT_START_TIME)
                .endDate(DEFAULT_END_TIME)
                .externalUserId(DEFAULT_EXTERNAL_USER_ID)
                .authorized(DEFAULT_AUTHORIZED);
    }

    public static RestSourceUserPropertiesDTO createDefaultDeviceDto() {
        return new RestSourceUserPropertiesDTO(createEntity());
    }

    @Before
    public void initTest() {
        sampleRestSourceUser = createEntity();
    }

    @Test
    @Transactional
    public void getAllUsers() throws Exception {
        // Initialize the database
        restSourceUserRepository.saveAndFlush(sampleRestSourceUser);

        // Get all the users
        restUserMockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.users.[*].projectId").value(hasItem(DEFAULT_PROJ_NAME)))
                .andExpect(jsonPath("$.users.[*].userId").value(hasItem(DEFAULT_USER_ID)))
                .andExpect(jsonPath("$.users.[*].sourceId").value(hasItem(DEFAULT_SOURCE_ID)))
                .andExpect(jsonPath("$.users.[*].externalUserId").value(
                        hasItem(DEFAULT_EXTERNAL_USER_ID.toString())));
    }


}
