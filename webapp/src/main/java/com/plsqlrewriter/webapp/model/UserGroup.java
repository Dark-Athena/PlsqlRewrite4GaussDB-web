package com.plsqlrewriter.webapp.model;

import jakarta.persistence.*;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @JsonManagedReference
    @OneToMany(mappedBy = "userGroup")
    private Set<com.plsqlrewriter.webapp.model.User> users;

    @OneToMany(mappedBy = "userGroup")
    @JsonIgnore
    private Set<Project> projects;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<com.plsqlrewriter.webapp.model.User> getUsers() {
        return users;
    }

    public void setUsers(Set<com.plsqlrewriter.webapp.model.User> users) {
        this.users = users;
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public void setProjects(Set<Project> projects) {
        this.projects = projects;
    }
} 