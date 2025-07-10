package com.plsqlrewriter.webapp.repository;

import com.plsqlrewriter.webapp.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
} 