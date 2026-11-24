package com.sportbuddy.sportbuddy.service;

import com.sportbuddy.sportbuddy.dto.CreateUserRequest;
import com.sportbuddy.sportbuddy.dto.CurrentUserResponse;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(CreateUserRequest request) {
        User user = new User(request.name(), request.email(), passwordEncoder.encode(request.password()));
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Adresa de email este deja folosita");
            }
            throw exception;
        }
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizatorul nu exista"));
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
