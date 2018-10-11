package org.radarbase.authorizer.service;

import java.util.List;
import java.util.stream.Collectors;

import org.radarbase.authorizer.domain.Device;
import org.radarbase.authorizer.repository.DeviceRepository;
import org.radarbase.authorizer.service.dto.DevicePropertiesDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviceService {
    private final Logger log = LoggerFactory.getLogger(DeviceService.class);

    @Autowired
    private DeviceRepository deviceRepository;


    public List<DevicePropertiesDTO> getAllDevices() {
        log.info("Querying all saved devices");
        return this.deviceRepository.findAll().stream()
                .map(DevicePropertiesDTO::new)
                .collect(Collectors.toList());
    }

    public DevicePropertiesDTO save(DevicePropertiesDTO devicePropertiesDTO) {
        Device device = this.deviceRepository.save(new Device(devicePropertiesDTO));
        return new DevicePropertiesDTO(device);
    }

    Device save(Device device) {
        log.info("Saving new device");
        return this.deviceRepository.save(device);
    }
}
