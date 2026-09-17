package com.preppilot.auth;

import com.preppilot.auth.AuthDtos.TokenResponse;
import com.preppilot.common.ApiException;
import com.preppilot.subscription.Subscription;
import com.preppilot.subscription.SubscriptionRepository;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, SubscriptionRepository subscriptions, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.subscriptions = subscriptions;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public TokenResponse register(String email, String password) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) {
            throw new ApiException(HttpStatus.CONFLICT, "email already registered");
        }
        User user = users.save(new User(normalized, encoder.encode(password)));
        subscriptions.save(new Subscription(user.getId()));   // every user starts on the free tier
        return new TokenResponse(jwt.issue(user.getId(), user.getEmail()), user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        return new TokenResponse(jwt.issue(user.getId(), user.getEmail()), user.getId(), user.getEmail());
    }
}
