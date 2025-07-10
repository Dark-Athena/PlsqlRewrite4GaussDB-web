package com.plsqlrewriter.webapp.repository;

import com.plsqlrewriter.webapp.model.Project;
import com.plsqlrewriter.webapp.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    // Spring Data JPA会根据方法名自动生成查询
    List<Project> findByOwner(String owner);
    List<Project> findByGroup(String group);
    List<Project> findByUserGroup(UserGroup userGroup);
} 