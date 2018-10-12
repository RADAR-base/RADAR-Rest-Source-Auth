package org.radarbase.authorizer.repository;

import java.util.Optional;

import org.radarbase.authorizer.domain.DeviceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceUserRepository extends JpaRepository<DeviceUser, Long> {
    Optional<DeviceUser> findByDeviceTypeAndExternalUserId(String deviceType,
            String externalUserId);
}
