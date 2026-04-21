package com.portal.identity.app.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.portal.identity.app.messaging.IdentityMessageSender;
import com.portal.identity.domain.Role;
import com.portal.identity.domain.RoleRepository;
import com.portal.identity.domain.User;
import com.portal.identity.domain.UserRepository;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IdentityMessageSender identityMessageSender;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, RoleRepository roleRepository, IdentityMessageSender identityMessageSender, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.identityMessageSender = identityMessageSender;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String password, String roleName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    return roleRepository.save(newRole);
                });
        user.setRoles(Set.of(role));
        
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        Map<String, String> headers = identityMessageSender.getDefaultMessageHeaders(saved.getId().toString());
        headers.put("userId", saved.getId().toString());
        headers.put("email", saved.getEmail());
        headers.put("role", roleName);

        identityMessageSender.sendUserRegistered(saved.getId().toString(), headers);

        return saved;
    }
}
