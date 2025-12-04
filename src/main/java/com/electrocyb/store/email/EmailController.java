package com.electrocyb.store.email;

import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@CrossOrigin // si ya tienes CORS global, puedes omitirlo
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest request) {
        try {
            emailService.sendHtmlEmail(request);
            return ResponseEntity.ok("Correo enviado correctamente");
        } catch (MessagingException e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body("Error al enviar el correo: " + e.getMessage());
        }
    }
}