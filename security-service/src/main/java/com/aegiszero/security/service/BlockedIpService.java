package com.aegiszero.security.service;

import com.aegiszero.common.exception.ConflictException;
import com.aegiszero.common.exception.ResourceNotFoundException;
import com.aegiszero.security.dto.BlockedIpRequest;
import com.aegiszero.security.dto.BlockedIpResponse;
import com.aegiszero.security.entity.BlockedIp;
import com.aegiszero.security.repository.BlockedIpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockedIpService {

    private final BlockedIpRepository blockedIpRepository;

    @Transactional(readOnly = true)
    public List<BlockedIpResponse> list() {
        return blockedIpRepository.findAll().stream().map(BlockedIpResponse::from).toList();
    }

    @Transactional
    public BlockedIpResponse block(BlockedIpRequest request) {
        if (blockedIpRepository.existsByIpAddress(request.ipAddress())) {
            throw new ConflictException("IP address is already blocked");
        }
        BlockedIp entity = BlockedIp.builder().ipAddress(request.ipAddress()).reason(request.reason()).build();
        return BlockedIpResponse.from(blockedIpRepository.save(entity));
    }

    @Transactional
    public void unblock(UUID id) {
        if (!blockedIpRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blocked IP entry not found");
        }
        blockedIpRepository.deleteById(id);
    }
}
