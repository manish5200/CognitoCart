package com.manish.smartcart.config.initializer;

import com.manish.smartcart.enums.ErrorCode;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class DataInitializer {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (usersRepository.existsByEmail(adminProperties.getEmail())) {
            log.info("Admin already exists.");
            return;
        }
        String normalizedPhone = PhoneUtil.normalize(adminProperties.getPhone(), "+91");
        if (usersRepository.existsByPhone(normalizedPhone)) {
            log.info("Phone already exists, having error: {}", ErrorCode.PHONE_ALREADY_EXISTS);
            return;
        }
        Users admin = Users.builder()
                .fullName(adminProperties.getName())
                .email(adminProperties.getEmail())
                .password(passwordEncoder.encode(adminProperties.getPassword()))
                .role(Role.ADMIN)
                .phone(normalizedPhone) // Stored in normalized E.164 format (+91XXXXXXXXXX)
                .active(true)
                .build();
        usersRepository.save(admin);

        log.info("Admin initialized: {}", adminProperties.getEmail());
    }
}
