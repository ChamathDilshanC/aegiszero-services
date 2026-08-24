package com.aegiszero.security.service;

import com.aegiszero.common.exception.InvalidCredentialsException;
import com.aegiszero.common.exception.ResourceNotFoundException;
import com.aegiszero.security.dto.*;
import com.aegiszero.security.entity.MfaMethod;
import com.aegiszero.security.entity.MfaMethodType;
import com.aegiszero.security.entity.RecoveryCode;
import com.aegiszero.security.event.SecurityEventPublisher;
import com.aegiszero.security.repository.MfaMethodRepository;
import com.aegiszero.security.repository.RecoveryCodeRepository;
import com.aegiszero.security.util.TotpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MfaService {

    private static final String CHALLENGE_KEY_PREFIX = "mfa:challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;
    private static final int RECOVERY_CODE_COUNT = 10;

    private final MfaMethodRepository mfaMethodRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventPublisher eventPublisher;
    private final CodeHasher codeHasher;
    private final String jwtIssuer;

    public MfaService(MfaMethodRepository mfaMethodRepository,
                       RecoveryCodeRepository recoveryCodeRepository,
                       RedisTemplate<String, Object> redisTemplate,
                       SecurityEventPublisher eventPublisher,
                       CodeHasher codeHasher,
                       @Value("${aegiszero.jwt.issuer:aegiszero}") String jwtIssuer) {
        this.mfaMethodRepository = mfaMethodRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.codeHasher = codeHasher;
        this.jwtIssuer = jwtIssuer;
    }

    @Transactional(readOnly = true)
    public MfaMethodsResponse getMethods(UUID userId) {
        List<String> methods = mfaMethodRepository.findByUserIdAndEnabledTrue(userId).stream()
                .map(m -> m.getType().name())
                .toList();
        return new MfaMethodsResponse(!methods.isEmpty(), methods);
    }

    @Transactional
    public TotpEnrollResponse enrollTotp(UUID userId, String email) {
        String secret = TotpUtil.generateBase32Secret();
        MfaMethod method = mfaMethodRepository.findByUserIdAndType(userId, MfaMethodType.TOTP)
                .orElseGet(() -> MfaMethod.builder().userId(userId).type(MfaMethodType.TOTP).build());
        method.setSecret(secret);
        method.setEnabled(false);
        mfaMethodRepository.save(method);

        String otpauthUrl = TotpUtil.buildOtpAuthUrl(jwtIssuer, email == null ? userId.toString() : email, secret);
        return new TotpEnrollResponse(secret, otpauthUrl);
    }

    @Transactional
    public RecoveryCodesResponse confirmTotp(UUID userId, String code) {
        MfaMethod method = mfaMethodRepository.findByUserIdAndType(userId, MfaMethodType.TOTP)
                .orElseThrow(() -> new ResourceNotFoundException("No pending TOTP enrollment"));

        if (!TotpUtil.verify(method.getSecret(), code)) {
            throw new InvalidCredentialsException("Invalid verification code");
        }

        method.setEnabled(true);
        mfaMethodRepository.save(method);
        eventPublisher.mfaEnabled(userId.toString(), "TOTP");

        return new RecoveryCodesResponse(regenerateRecoveryCodes(userId));
    }

    @Transactional
    public void enableEmailOtp(UUID userId) {
        MfaMethod method = mfaMethodRepository.findByUserIdAndType(userId, MfaMethodType.EMAIL_OTP)
                .orElseGet(() -> MfaMethod.builder().userId(userId).type(MfaMethodType.EMAIL_OTP).build());
        method.setEnabled(true);
        mfaMethodRepository.save(method);
        eventPublisher.mfaEnabled(userId.toString(), "EMAIL_OTP");
    }

    @Transactional
    public void disable(UUID userId, MfaMethodType type) {
        mfaMethodRepository.findByUserIdAndType(userId, type).ifPresent(mfaMethodRepository::delete);
        eventPublisher.mfaDisabled(userId.toString(), type.name());
    }

    public MfaChallengeResponse createChallenge(MfaChallengeRequest request) {
        String challengeId = UUID.randomUUID().toString();
        String codeHash = null;

        if ("EMAIL_OTP".equals(request.method())) {
            String code = codeHasher.generateNumericCode(6);
            codeHash = codeHasher.hash(code);
            eventPublisher.notify(request.email(), "MFA_OTP", "Your AegisZero verification code",
                    Map.of("code", code));
        }

        MfaChallenge challenge = new MfaChallenge(request.userId(), request.method(), codeHash, 0);
        redisTemplate.opsForValue().set(challengeKey(challengeId), challenge, CHALLENGE_TTL);

        return new MfaChallengeResponse(challengeId, request.method(), request.userId());
    }

    @Transactional
    public MfaVerifyInternalResponse verifyChallenge(MfaVerifyInternalRequest request) {
        String key = challengeKey(request.challengeId());
        MfaChallenge challenge = (MfaChallenge) redisTemplate.opsForValue().get(key);

        if (challenge == null) {
            return new MfaVerifyInternalResponse(false, null);
        }
        if (challenge.getAttempts() >= MAX_ATTEMPTS) {
            redisTemplate.delete(key);
            return new MfaVerifyInternalResponse(false, null);
        }

        UUID userId = UUID.fromString(challenge.getUserId());
        boolean verified = verifyCode(userId, challenge, request.code());

        if (!verified) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            Long ttl = redisTemplate.getExpire(key);
            redisTemplate.opsForValue().set(key, challenge, Duration.ofSeconds(ttl == null || ttl < 0 ? CHALLENGE_TTL.toSeconds() : ttl));
            return new MfaVerifyInternalResponse(false, null);
        }

        redisTemplate.delete(key);
        return new MfaVerifyInternalResponse(true, challenge.getUserId());
    }

    private boolean verifyCode(UUID userId, MfaChallenge challenge, String code) {
        if ("EMAIL_OTP".equals(challenge.getMethod())) {
            if (challenge.getCodeHash() != null && challenge.getCodeHash().equals(codeHasher.hash(code))) {
                return true;
            }
        } else if ("TOTP".equals(challenge.getMethod())) {
            var method = mfaMethodRepository.findByUserIdAndType(userId, MfaMethodType.TOTP).orElse(null);
            if (method != null && method.isEnabled() && TotpUtil.verify(method.getSecret(), code)) {
                return true;
            }
        }
        return tryRecoveryCode(userId, code);
    }

    private boolean tryRecoveryCode(UUID userId, String code) {
        String hash = codeHasher.hash(code.trim().toUpperCase());
        return recoveryCodeRepository.findByUserIdAndCodeHash(userId, hash)
                .filter(rc -> !rc.isUsed())
                .map(rc -> {
                    rc.setUsed(true);
                    recoveryCodeRepository.save(rc);
                    return true;
                })
                .orElse(false);
    }

    private List<String> regenerateRecoveryCodes(UUID userId) {
        recoveryCodeRepository.deleteByUserId(userId);
        List<String> rawCodes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String raw = codeHasher.generateRecoveryCode();
            rawCodes.add(raw);
            recoveryCodeRepository.save(RecoveryCode.builder()
                    .userId(userId)
                    .codeHash(codeHasher.hash(raw))
                    .build());
        }
        return rawCodes;
    }

    private String challengeKey(String challengeId) {
        return CHALLENGE_KEY_PREFIX + challengeId;
    }
}
