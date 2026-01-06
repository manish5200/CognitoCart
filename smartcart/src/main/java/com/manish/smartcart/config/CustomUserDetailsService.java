package com.manish.smartcart.config;

import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;
    public CustomUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Users> usersOptional = usersRepository.findByEmail(username);
        Users user = usersOptional.orElseThrow(()->new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(user);
    }
}
