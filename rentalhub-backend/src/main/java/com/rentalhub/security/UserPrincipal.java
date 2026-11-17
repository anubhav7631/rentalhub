package com.rentalhub.security;

import com.rentalhub.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;


public class UserPrincipal implements UserDetails {

    
    @Getter
    private final Long id;

    @Getter
    private final User user;

    private final String email;
    private final String password;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;
    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = user.getRoles()
            .stream()
            .map(role -> new SimpleGrantedAuthority(role.getName().name()))
            .collect(Collectors.toSet());

        return new UserPrincipal(
            user.getId(),
            user,
            user.getEmail(),
            user.getPassword(),
            user.getIsActive(),
            authorities
        );
    }

    private UserPrincipal(Long id, User user, String email,
                          String password, boolean isActive,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id           = id;
        this.user         = user;
        this.email        = email;
        this.password     = password;
        this.isActive     = isActive;
        this.authorities  = authorities;
    }

    // ── UserDetails interface implementation ─────────────────────

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive;
    }
}