package com.skillgapai.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skillgapai.domain.User;
import com.skillgapai.repository.UserRepository;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final List<String> SUPPORTED_ROLES = List.of("Backend Developer", "Data Analyst", "Cloud Engineer");

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/target-role")
    public TargetRoleResponse getTargetRole(Authentication authentication) {
        return new TargetRoleResponse(currentUser(authentication).getTargetRole());
    }

    @PutMapping("/target-role")
    public TargetRoleResponse saveTargetRole(Authentication authentication, @Valid @RequestBody TargetRoleRequest request) {
        if (!SUPPORTED_ROLES.contains(request.targetRole().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a supported target role");
        }
        User user = currentUser(authentication);
        user.setTargetRole(request.targetRole().trim());
        users.save(user);
        return new TargetRoleResponse(user.getTargetRole());
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record TargetRoleRequest(@NotBlank String targetRole) {}
    public record TargetRoleResponse(String targetRole) {}
}