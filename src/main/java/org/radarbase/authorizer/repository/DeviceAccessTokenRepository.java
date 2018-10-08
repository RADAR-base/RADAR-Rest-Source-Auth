package org.radarbase.authorizer.repository;

import org.radarbase.authorizer.domain.DeviceAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceAccessTokenRepository extends JpaRepository<DeviceAccessToken, Long> {
}
