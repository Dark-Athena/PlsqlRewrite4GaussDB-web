package com.plsqlrewriter.webapp;

import com.plsqlrewriter.webapp.model.User;
import com.plsqlrewriter.webapp.repository.UserRepository;
import com.plsqlrewriter.webapp.model.UserGroup;
import com.plsqlrewriter.webapp.repository.UserGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, UserGroupRepository userGroupRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userGroupRepository.count() == 0) {
            UserGroup defaultGroup = new UserGroup();
            defaultGroup.setName("Default");
            userGroupRepository.save(defaultGroup);
            logger.info("[DataInitializer] Default user group initialized");
        }

        if (userRepository.count() == 0) {
            UserGroup defaultGroup = userGroupRepository.findAll().get(0);
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRoles("ADMIN");
            admin.setUserGroup(defaultGroup);
            userRepository.save(admin);
            logger.info("[DataInitializer] Admin user initialized, password is 'password'");
        } else {
            logger.info("[DataInitializer] User table is not empty, admin user not initialized");
        }
    }
} 