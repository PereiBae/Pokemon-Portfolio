package com.pokemonportfolio.auth.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OwnerBootstrapService implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String displayName;
    private final String bootstrapPassword;

    public OwnerBootstrapService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.owner.username}") String username,
            @Value("${app.owner.display-name}") String displayName,
            @Value("${app.owner.bootstrap-password:}") String bootstrapPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.displayName = displayName;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.existsByUsername(username) || !StringUtils.hasText(bootstrapPassword)) {
            return;
        }
        appUserRepository.save(new AppUser(
                username,
                passwordEncoder.encode(bootstrapPassword),
                displayName,
                "ROLE_OWNER"));
    }
}

