package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.webapp.model.UserGroup;
import com.plsqlrewriter.webapp.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/api/groups")
public class UserGroupController {

    @Autowired
    private UserGroupRepository userGroupRepository;

    @GetMapping
    public List<UserGroup> getAllGroups() {
        return userGroupRepository.findAll();
    }

    @PostMapping
    public UserGroup createGroup(@RequestBody UserGroup userGroup) {
        return userGroupRepository.save(userGroup);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserGroup> updateGroup(@PathVariable Long id, @RequestBody UserGroup groupDetails) {
        return userGroupRepository.findById(id)
                .map(group -> {
                    group.setName(groupDetails.getName());
                    return ResponseEntity.ok(userGroupRepository.save(group));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        return userGroupRepository.findById(id)
                .map(group -> {
                    userGroupRepository.delete(group);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 