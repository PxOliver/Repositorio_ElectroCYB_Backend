package com.electrocyb.store.auth;

import com.electrocyb.store.auth.dto.*;
import com.electrocyb.store.email.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       VerificationTokenRepository verificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    // 🔹 REGISTRO: crea usuario deshabilitado y envía correo de verificación
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(Role.CUSTOMER)
                .enabled(false) // 👈 importante: deshabilitado hasta verificar
                .build();

        userRepository.save(user);

        // Generar token de verificación
        VerificationToken vt = new VerificationToken();
        vt.setToken(UUID.randomUUID().toString());
        vt.setUsuario(user);
        vt.setExpiryDate(Instant.now().plus(24, ChronoUnit.HOURS)); // 24h de validez
        verificationTokenRepository.save(vt);

        // Enviar correo de verificación
        try {
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    user.getFullName(),
                    vt.getToken()
            );
        } catch (MessagingException e) {
            // No rompemos el flujo de registro, solo log
            e.printStackTrace();
        }
    }

    // 🔹 VERIFICAR CUENTA (desde enlace del email)
    public void verifyAccount(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de verificación inválido")
                );

        if (vt.getExpiryDate().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de verificación ha expirado");
        }

        User user = vt.getUsuario();
        user.setEnabled(true);
        userRepository.save(user);

        // Eliminamos el token para que no se reutilice
        verificationTokenRepository.delete(vt);
    }

    // 🔹 LOGIN (solo permite usuarios habilitados)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes verificar tu correo antes de iniciar sesión."
            );
        }

        // Notificación de inicio de sesión (opcional)
        try {
            emailService.sendLoginNotification(
                    user.getEmail(),
                    user.getFullName()
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, mapToDto(user));
    }

    public UserDto mapToDto(User u) {
        return new UserDto(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole().name()
        );
    }
}