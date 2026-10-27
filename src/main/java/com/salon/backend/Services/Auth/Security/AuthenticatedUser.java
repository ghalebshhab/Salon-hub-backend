package com.salon.backend.Services.Auth.Security;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.Instant;

public final class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final List<GrantedAuthority> authorities;
    private final boolean enabled;
    private final Instant credentialsUpdatedAt;

    private AuthenticatedUser(
            Long id,
            String email,
            String password,
            List<GrantedAuthority> authorities,
            boolean enabled,
            Instant credentialsUpdatedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.credentialsUpdatedAt = credentialsUpdatedAt;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.getStatus() == UserStatus.Active,
                user.getCredentialsUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCredentialsUpdatedAt() {
        return credentialsUpdatedAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
