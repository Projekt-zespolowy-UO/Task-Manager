package com.project.backend.Security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.backend.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
@Service
@RequiredArgsConstructor
public class CustomUserServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;

    

    public UserDetails loadUserByUsername(String email)


    
            throws UsernameNotFoundException {

        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
