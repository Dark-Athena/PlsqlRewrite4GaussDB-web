package com.plsqlrewriter.webapp.controller;

import com.plsqlrewriter.webapp.model.User;
import com.plsqlrewriter.webapp.model.UserGroup;
import com.plsqlrewriter.webapp.model.request.BatchDeleteRequest;
import com.plsqlrewriter.webapp.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class UserGroupController {

    @Autowired
    private UserGroupRepository userGroupRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserGroup> getAllGroups() {
        return userGroupRepository.findAll();
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, "application/json;charset=UTF-8"}, 
                produces = MediaType.APPLICATION_JSON_VALUE)
    public UserGroup createGroup(@RequestBody UserGroup userGroup) {
        return userGroupRepository.save(userGroup);
    }

    @PutMapping(value = "/{id}", 
                consumes = {MediaType.APPLICATION_JSON_VALUE, "application/json;charset=UTF-8"}, 
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserGroup> updateGroup(@PathVariable Long id, @RequestBody UserGroup groupDetails) {
        return userGroupRepository.findById(id)
                .map(group -> {
                    group.setName(groupDetails.getName());
                    return ResponseEntity.ok(userGroupRepository.save(group));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        return userGroupRepository.findById(id)
                .map(group -> {
                    userGroupRepository.delete(group);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 