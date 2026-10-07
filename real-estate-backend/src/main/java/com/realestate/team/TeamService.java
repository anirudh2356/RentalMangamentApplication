package com.realestate.team;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.team.dto.TeamMemberResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TeamService {

    private final UserRepository userRepository;

    public TeamService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> members(String email, String search, Role role, Boolean enabled) {
        User broker = requireBroker(email);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return userRepository.findTeamMembers(broker.getId(), role, enabled, normalizedSearch)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User requireBroker(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required"));
        if (!user.isEnabled() || user.getRole() != Role.BROKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private TeamMemberResponse toResponse(User user) {
        return new TeamMemberResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
