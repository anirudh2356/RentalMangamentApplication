package com.realestate.followup;

import com.realestate.followup.dto.FollowUpRequest;
import com.realestate.followup.dto.FollowUpResponse;
import com.realestate.followup.model.FollowUpPriority;
import com.realestate.followup.model.FollowUpStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/followups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public List<FollowUpResponse> list(
            Authentication authentication,
            @RequestParam(required = false) FollowUpStatus status,
            @RequestParam(required = false) FollowUpPriority priority,
            @RequestParam(required = false) Long leadId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search
    ) {
        return followUpService.list(authentication.getName(), status, priority, leadId, from, to, search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse create(
            Authentication authentication,
            @Valid @RequestBody FollowUpRequest request
    ) {
        return followUpService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public FollowUpResponse get(Authentication authentication, @PathVariable Long id) {
        return followUpService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public FollowUpResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody FollowUpRequest request
    ) {
        return followUpService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        followUpService.delete(authentication.getName(), id);
    }
}
