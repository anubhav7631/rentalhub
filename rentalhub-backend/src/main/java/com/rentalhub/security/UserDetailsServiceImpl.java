package com.rentalhub.security;

import com.rentalhub.entity.User;
import com.rentalhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.debug("Loading user by email: {}", username);

        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> {
                log.warn("User not found with email: {}", username);
                return new UsernameNotFoundException(
                    "User not found with email: " + username
                );
            });

        log.debug("User found: {} with {} roles", user.getEmail(), user.getRoles().size());

        return UserPrincipal.create(user);
    }
}