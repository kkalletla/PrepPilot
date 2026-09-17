package com.preppilot.auth;

import com.preppilot.auth.AuthDtos.LoginRequest;
import com.preppilot.auth.AuthDtos.MeResponse;
import com.preppilot.auth.AuthDtos.RegisterRequest;
import com.preppilot.auth.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req.email(), req.password());
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new MeResponse(user.id(), user.email());
    }
}
