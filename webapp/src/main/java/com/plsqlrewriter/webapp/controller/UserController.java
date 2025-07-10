package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.webapp.model.User;
import com.plsqlrewriter.webapp.repository.UserRepository;
import com.plsqlrewriter.webapp.model.UserGroup;
import com.plsqlrewriter.webapp.model.request.UserCreationRequest;
import com.plsqlrewriter.webapp.model.request.UserUpdateRequest;
import com.plsqlrewriter.webapp.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserCreationRequest request) {
        UserGroup userGroup = userGroupRepository.findById(request.getUserGroupId())
                .orElse(null);

        if (userGroup == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or handle error appropriately
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setUserGroup(userGroup);
        newUser.setRoles("USER"); // Default role

        User savedUser = userRepository.save(newUser);
        return ResponseEntity.ok(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(request.getUsername());
                    user.setRoles(request.getRoles());

                    if (request.getUserGroupId() != null) {
                        UserGroup userGroup = userGroupRepository.findById(request.getUserGroupId()).orElse(null);
                        if (userGroup == null) {
                            // Or throw an exception
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                        }
                        user.setUserGroup(userGroup);
                    }
                    
                    User updatedUser = userRepository.save(user);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 