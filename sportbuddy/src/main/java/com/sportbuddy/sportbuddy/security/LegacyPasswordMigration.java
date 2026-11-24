package com.sportbuddy.sportbuddy.security;

import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyPasswordMigration implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LegacyPasswordMigration(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (User user : userRepository.findAllByPasswordEncodedIsNull()) {
            user.updatePasswordHash(passwordEncoder.encode(user.getPassword()));
        }
    }
}
