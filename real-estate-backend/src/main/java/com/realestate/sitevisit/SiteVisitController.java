package com.realestate.sitevisit;

import com.realestate.sitevisit.dto.SiteVisitRequest;
import com.realestate.sitevisit.dto.SiteVisitResponse;
import com.realestate.sitevisit.model.SiteVisitStatus;
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
@RequestMapping("/api/site-visits")
public class SiteVisitController {

    private final SiteVisitService siteVisitService;

    public SiteVisitController(SiteVisitService siteVisitService) {
        this.siteVisitService = siteVisitService;
    }

    @GetMapping
    public List<SiteVisitResponse> list(
            Authentication authentication,
            @RequestParam(required = false) SiteVisitStatus status
    ) {
        return siteVisitService.list(authentication.getName(), status);
    }

    @GetMapping("/upcoming")
    public List<SiteVisitResponse> upcoming(Authentication authentication) {
        return siteVisitService.upcoming(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SiteVisitResponse create(
            Authentication authentication,
            @Valid @RequestBody SiteVisitRequest request
    ) {
        return siteVisitService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public SiteVisitResponse get(Authentication authentication, @PathVariable Long id) {
        return siteVisitService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public SiteVisitResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SiteVisitRequest request
    ) {
        return siteVisitService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        siteVisitService.delete(authentication.getName(), id);
    }
}
