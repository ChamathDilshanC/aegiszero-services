package com.aegiszero.security.controller;

import com.aegiszero.security.dto.RiskEvaluationRequest;
import com.aegiszero.security.dto.RiskEvaluationResponse;
import com.aegiszero.security.service.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security/internal/risk")
@RequiredArgsConstructor
public class InternalRiskController {

    private final RiskService riskService;

    @PostMapping("/evaluate")
    public RiskEvaluationResponse evaluate(@Valid @RequestBody RiskEvaluationRequest request) {
        return riskService.evaluate(request);
    }
}
