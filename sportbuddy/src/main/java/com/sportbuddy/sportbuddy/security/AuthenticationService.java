package com.sportbuddy.sportbuddy.security;

import com.sportbuddy.sportbuddy.dto.LoginRequest;
import com.sportbuddy.sportbuddy.dto.LoginResponse;
import com.sportbuddy.sportbuddy.model.User;
import com.sportbuddy.sportbuddy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String unavailableAccountHash;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.unavailableAccountHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> account = userRepository.findByEmail(request.email()).filter(User::hasPasswordHash);
        String hash = account.map(User::getPassword).orElse(unavailableAccountHash);
        if (!passwordEncoder.matches(request.password(), hash) || account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email sau parola incorecta");
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfiguration.TOKEN_ISSUER)
                .subject(account.get().getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds());
    }
}
