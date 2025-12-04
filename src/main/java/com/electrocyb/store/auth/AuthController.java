package com.electrocyb.store.auth;

import com.electrocyb.store.auth.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // 🔹 Registro: solo devuelve un mensaje. El frontend no necesita token aquí.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(
                Map.of("message", "Registro exitoso. Revisa tu correo para activar tu cuenta.")
        );
    }

    // 🔹 Login: sí devuelve token + usuario
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 🔹 Verificar cuenta (llamado desde el link del email)
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        authService.verifyAccount(token);
        return ResponseEntity.ok(
                Map.of("message", "Cuenta verificada correctamente. Ya puedes iniciar sesión.")
        );
    }

    // 🔹 Usuario actual (para /auth/me)
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();
        return authService.mapToDto(user);
    }
}
