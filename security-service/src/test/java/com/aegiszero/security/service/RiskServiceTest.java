package com.aegiszero.security.service;

import com.aegiszero.security.dto.RiskEvaluationRequest;
import com.aegiszero.security.dto.RiskEvaluationResponse;
import com.aegiszero.security.entity.Device;
import com.aegiszero.security.event.SecurityEventPublisher;
import com.aegiszero.security.repository.BlockedIpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private BlockedIpRepository blockedIpRepository;
    @Mock
    private SecurityEventPublisher eventPublisher;

    private RiskService riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskService(deviceService, blockedIpRepository, eventPublisher);
    }

    private RiskEvaluationRequest request(String ip) {
        return new RiskEvaluationRequest(UUID.randomUUID().toString(), ip, "curl/8", "fp-1", "Test Device");
    }

    private Device deviceWith(boolean blocked, boolean trusted) {
        return Device.builder().id(UUID.randomUUID()).blocked(blocked).trusted(trusted).build();
    }

    @Test
    void newDevice_fromCleanIp_isAllowedWithModerateScore() {
        Device device = deviceWith(false, false);
        when(deviceService.resolveForLogin(any(), anyString(), anyString(), anyString()))
                .thenReturn(new DeviceService.Resolution(device, true));
        when(blockedIpRepository.existsByIpAddress(anyString())).thenReturn(false);

        RiskEvaluationResponse response = riskService.evaluate(request("1.2.3.4"));

        assertThat(response.riskScore()).isEqualTo(25);
        assertThat(response.decision()).isEqualTo("ALLOW");
        assertThat(response.newDevice()).isTrue();
        assertThat(response.reasons()).containsExactly("NEW_DEVICE");
    }

    @Test
    void trustedKnownDevice_scoresBelowZero_clampedToZero() {
        Device device = deviceWith(false, true);
        when(deviceService.resolveForLogin(any(), anyString(), anyString(), anyString()))
                .thenReturn(new DeviceService.Resolution(device, false));
        when(blockedIpRepository.existsByIpAddress(anyString())).thenReturn(false);

        RiskEvaluationResponse response = riskService.evaluate(request("1.2.3.4"));

        assertThat(response.riskScore()).isZero();
        assertThat(response.decision()).isEqualTo("ALLOW");
    }

    @Test
    void blockedDevice_isBlockedRegardlessOfOtherFactors() {
        Device device = deviceWith(true, false);
        when(deviceService.resolveForLogin(any(), anyString(), anyString(), anyString()))
                .thenReturn(new DeviceService.Resolution(device, false));

        RiskEvaluationResponse response = riskService.evaluate(request("1.2.3.4"));

        assertThat(response.decision()).isEqualTo("BLOCK");
        assertThat(response.reasons()).containsExactly("DEVICE_BLOCKED");
    }

    @Test
    void newDeviceFromBlockedIp_scoresAboveCriticalThreshold_isBlocked() {
        // 25 (new device) + 80 (blocked IP) = 105, which is >= the 91-point BLOCK threshold.
        Device device = deviceWith(false, false);
        when(deviceService.resolveForLogin(any(), anyString(), anyString(), anyString()))
                .thenReturn(new DeviceService.Resolution(device, true));
        when(blockedIpRepository.existsByIpAddress(anyString())).thenReturn(true);

        RiskEvaluationResponse response = riskService.evaluate(request("6.6.6.6"));

        assertThat(response.riskScore()).isEqualTo(105);
        assertThat(response.decision()).isEqualTo("BLOCK");
        assertThat(response.reasons()).containsExactlyInAnyOrder("NEW_DEVICE", "BLOCKED_IP");
    }

    @Test
    void knownUntrustedDevice_fromCleanIp_scoresZero_allowed() {
        Device device = deviceWith(false, false);
        when(deviceService.resolveForLogin(any(), anyString(), anyString(), anyString()))
                .thenReturn(new DeviceService.Resolution(device, false));
        when(blockedIpRepository.existsByIpAddress(anyString())).thenReturn(false);

        RiskEvaluationResponse response = riskService.evaluate(request("1.2.3.4"));

        assertThat(response.riskScore()).isZero();
        assertThat(response.decision()).isEqualTo("ALLOW");
        assertThat(response.reasons()).isEmpty();
    }
}
