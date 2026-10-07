package com.realestate.deals;

import com.realestate.deals.dto.DealRequest;
import com.realestate.deals.dto.DealResponse;
import com.realestate.deals.model.DealStage;
import com.realestate.deals.model.DealStatus;
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
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public List<DealResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DealStage stage,
            @RequestParam(required = false) DealStatus status
    ) {
        return dealService.list(authentication.getName(), search, stage, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealResponse create(Authentication authentication, @Valid @RequestBody DealRequest request) {
        return dealService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public DealResponse get(Authentication authentication, @PathVariable Long id) {
        return dealService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public DealResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody DealRequest request
    ) {
        return dealService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        dealService.delete(authentication.getName(), id);
    }
}
