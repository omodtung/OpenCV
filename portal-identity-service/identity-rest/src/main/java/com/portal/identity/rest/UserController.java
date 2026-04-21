package com.portal.identity.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.identity.app.service.UserRegistrationService;
import com.portal.identity.domain.User;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRegistrationService userRegistrationService;

    public UserController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> request) {
        User user = userRegistrationService.registerUser(
                request.get("email"),
                request.get("passwordHash"),
                request.get("roleName")
        );
        return ResponseEntity.ok(user);
    }
}