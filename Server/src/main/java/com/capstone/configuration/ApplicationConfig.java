package com.capstone.configuration;

import com.capstone.data.OwnerRepositoryJPA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class ApplicationConfig {

    private final OwnerRepositoryJPA repository;

    @Autowired
    public ApplicationConfig(OwnerRepositoryJPA repository) {
        this.repository = repository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> repository.getOwnerByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
