package com.realestate.commissions;

import com.realestate.commissions.dto.CommissionSummaryResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commissions")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping
    public CommissionSummaryResponse summary(Authentication authentication) {
        return commissionService.summary(authentication.getName());
    }
}
