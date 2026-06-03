package com.pokemonportfolio.auth.service;

import com.pokemonportfolio.auth.entity.AppUser;
import com.pokemonportfolio.auth.repository.AppUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Owner account not found"));
        return new User(
                appUser.getUsername(),
                appUser.getPasswordHash(),
                appUser.isEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(appUser.getRole())));
    }
}

