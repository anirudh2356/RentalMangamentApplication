package com.realestate.leads;

import com.realestate.leads.dto.LeadRequest;
import com.realestate.leads.dto.LeadResponse;
import com.realestate.leads.model.LeadSource;
import com.realestate.leads.model.LeadStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public List<LeadResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadSource source,
            @RequestParam(required = false) Long interestedPropertyId
    ) {
        return leadService.list(
                authentication.getName(),
                search,
                status,
                source,
                interestedPropertyId
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeadResponse create(
            Authentication authentication,
            @Valid @RequestBody LeadRequest request
    ) {
        return leadService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public LeadResponse get(Authentication authentication, @PathVariable Long id) {
        return leadService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public LeadResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody LeadRequest request
    ) {
        return leadService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        leadService.delete(authentication.getName(), id);
    }
}
