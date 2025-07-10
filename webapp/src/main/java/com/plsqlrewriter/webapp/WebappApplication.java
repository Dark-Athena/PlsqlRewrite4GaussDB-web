package com.plsqlrewriter.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.plsqlrewriter.webapp",
        "com.plsqlrewriter.webapp"
})
@EnableJpaRepositories(basePackages = {
        "com.plsqlrewriter.webapp.repository",
        "com.plsqlrewriter.webapp.repository"
})
@EntityScan(basePackages = {
        "com.plsqlrewriter.webapp.model",
        "com.plsqlrewriter.webapp.model"
})
public class WebappApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebappApplication.class, args);
    }
} 