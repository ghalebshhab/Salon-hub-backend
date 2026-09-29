package com.salon.backend.Services.Auth.Security;


import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        User user=userRepo.findByuserName(normalizedUsername);
        if(user==null){
            throw new UsernameNotFoundException("user with this user name dose not exist .");
        }
        return AuthenticatedUser.from(user);
    }
    public UserDetails loadByEmail(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        User user=userRepo.findByemail(normalizedEmail);
        if(user==null){
            throw new UsernameNotFoundException("user with this Email dose not exist .");
        }
        return AuthenticatedUser.from(user);
    }

    public AuthenticatedUser loadById(Long userId) throws UsernameNotFoundException {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User does not exist."));

        return AuthenticatedUser.from(user);
    }
}
