package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.LoginRequest;
import br.lcn.ragkb.dto.LoginResponse;
import br.lcn.ragkb.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        JwtService.AuthResult result = jwtService.authenticate(request.username(), request.password());
        return new LoginResponse(result.token(), result.expiresIn());
    }
}