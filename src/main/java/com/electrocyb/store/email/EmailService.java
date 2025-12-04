package com.electrocyb.store.email;

import com.electrocyb.store.pedido.Pedido;
import com.electrocyb.store.pedido.OrderItem;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class EmailService {

    private final SendGrid sendGrid;

    @Value("${mail.from}")
    private String fromEmail;

    @Value("${mail.from.name:ElectroCYB}")
    private String fromName;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    // Inyectamos la API key de SendGrid desde application.properties / env
    public EmailService(@Value("${sendgrid.api-key}") String apiKey) {
        this.sendGrid = new SendGrid(apiKey);
    }

    // =====================================
    // MÉTODO GENERAL PARA ENVIAR EMAIL HTML
    // =====================================
    public void sendHtmlEmail(EmailRequest request) throws MessagingException {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(request.to());

        Content content = new Content("text/html", request.html());
        Mail mail = new Mail(from, request.subject(), to, content);

        Request sgRequest = new Request();
        sgRequest.setMethod(Method.POST);
        sgRequest.setEndpoint("mail/send");

        try {
            sgRequest.setBody(mail.build());
            Response response = sendGrid.api(sgRequest);

            int status = response.getStatusCode();
            if (status >= 400) {
                log.error("Error al enviar correo con SendGrid. Status: {}, body: {}",
                        status, response.getBody());
                throw new MessagingException("Error SendGrid: " + status + " - " + response.getBody());
            }

        } catch (IOException e) {
            log.error("Error IO al enviar correo con SendGrid", e);
            throw new MessagingException("Error enviando correo con SendGrid", e);
        }
    }

    // =====================================
    // CORREO DE CONFIRMACIÓN DE PEDIDO
    // =====================================
    public void sendOrderConfirmation(Pedido pedido) throws MessagingException {
        if (pedido.getCliente() == null || pedido.getCliente().getEmail() == null) {
            return;
        }

        String html = buildOrderHtml(
                pedido,
                "Confirmación de Pedido",
                "Hemos recibido tu pedido y lo estamos procesando."
        );

        EmailRequest req = new EmailRequest(
                pedido.getCliente().getEmail(),
                "Confirmación de tu pedido " + pedido.getNumeroPedido(),
                html
        );

        sendHtmlEmail(req);
    }

    // =====================================
    // CORREO DE PEDIDO ENTREGADO
    // =====================================
    public void sendOrderDeliveredEmail(Pedido pedido) throws MessagingException {
        if (pedido.getCliente() == null || pedido.getCliente().getEmail() == null) {
            return;
        }

        String html = buildOrderHtml(
                pedido,
                "Pedido entregado",
                "Tu pedido ha sido entregado correctamente. ¡Esperamos que disfrutes tu compra!"
        );

        EmailRequest req = new EmailRequest(
                pedido.getCliente().getEmail(),
                "Tu pedido " + pedido.getNumeroPedido() + " ha sido ENTREGADO",
                html
        );

        sendHtmlEmail(req);
    }

    // =====================================
    // CORREO DE REGISTRO
    // =====================================
    public void sendRegistrationConfirmation(String email, String nombre)
            throws MessagingException {

        String html = "<h2>¡Bienvenido a ElectroCYB! ⚡</h2>"
                + "<p>Hola <strong>" + safe(nombre) + "</strong>,</p>"
                + "<p>Tu cuenta ha sido creada con éxito.</p>"
                + "<p>Ya puedes iniciar sesión y disfrutar de nuestros productos.</p>"
                + "<br><p>Gracias por confiar en ElectroCYB.</p>";

        EmailRequest req = new EmailRequest(
                email,
                "✔ Registro Exitoso – ElectroCYB",
                html
        );

        sendHtmlEmail(req);
    }

    // =====================================
    // CORREO DE INICIO DE SESIÓN
    // =====================================
    public void sendLoginNotification(String email, String nombre)
            throws MessagingException {

        String html = "<h2>Inicio de sesión detectado</h2>"
                + "<p>Hola <strong>" + safe(nombre) + "</strong>,</p>"
                + "<p>Hemos registrado un inicio de sesión en tu cuenta.</p>"
                + "<p>Si fuiste tú, ignora este mensaje.</p>"
                + "<p>Si NO reconoces este acceso, cambia tu contraseña.</p>"
                + "<br><p>Equipo de Seguridad – ElectroCYB ⚡</p>";

        EmailRequest req = new EmailRequest(
                email,
                "🔔 Nuevo inicio de sesión en tu cuenta",
                html
        );

        sendHtmlEmail(req);
    }

    // =====================================
    // CORREO DE VERIFICACIÓN DE CUENTA
    // =====================================
    public void sendVerificationEmail(String email, String nombre, String token)
            throws MessagingException {

        String verifyUrl = frontendBaseUrl + "/verificar?token=" + token;

        String html = """
            <html>
            <body style='font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;'>
              <div style='max-width:600px;margin:auto;background:white;border-radius:10px;border:1px solid #ddd;overflow:hidden;'>
                <div style='background:#4f46e5;color:white;padding:16px 24px;'>
                  <h2 style='margin:0;'>ElectroCYB – Verifica tu correo</h2>
                </div>
                <div style='padding:24px;'>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Gracias por registrarte en <strong>ElectroCYB</strong>.</p>
                  <p>Por favor haz clic en el siguiente botón para <strong>activar tu cuenta</strong>:</p>
                  <p style='text-align:center;margin:24px 0;'>
                    <a href='%s'
                       style='background:#4f46e5;color:white;padding:12px 24px;border-radius:999px;
                              text-decoration:none;font-weight:bold;display:inline-block;'>
                      Verificar mi cuenta
                    </a>
                  </p>
                  <p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                  <p style='font-size:12px;color:#555;word-break:break-all;'>%s</p>
                  <br>
                  <p>Si tú no creaste esta cuenta, puedes ignorar este correo.</p>
                  <p>— Equipo ElectroCYB ⚡</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(safe(nombre), verifyUrl, verifyUrl);

        EmailRequest req = new EmailRequest(
                email,
                "Verifica tu cuenta – ElectroCYB",
                html
        );

        sendHtmlEmail(req);
    }

    // =====================================
    // HTML DEL PEDIDO (GENÉRICO)
    // =====================================
    private String buildOrderHtml(Pedido pedido, String titulo, String subtitulo) {

        StringBuilder itemsHtml = new StringBuilder();

        for (OrderItem item : pedido.getItems()) {
            itemsHtml.append("<tr>")
                    .append("<td style='padding:8px;border-bottom:1px solid #eee;'>")
                    .append(safe(item.getNombre()))
                    .append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #eee;text-align:center;'>")
                    .append(item.getCantidad())
                    .append("</td>")
                    .append("<td style='padding:8px;border-bottom:1px solid #eee;text-align:right;'>S/ ")
                    .append(safe(item.getPrecio()))
                    .append("</td>")
                    .append("</tr>");
        }

        return "<html>"
                + "<body style='font-family:Arial,sans-serif;background:#f5f5f5;padding:20px;'>"
                + "<div style='max-width:600px;margin:auto;background:white;border-radius:10px;"
                + "border:1px solid #ddd;overflow:hidden;'>"
                + "<div style='background:#4f46e5;color:white;padding:16px 24px;'>"
                + "<h2 style='margin:0;'>ElectroCYB – " + safe(titulo) + "</h2>"
                + "</div>"
                + "<div style='padding:24px;'>"
                + "<p>Hola <strong>" + safe(pedido.getCliente().getNombre()) + "</strong>,</p>"
                + "<p>" + safe(subtitulo) + "</p>"
                + "<h3>Código: " + safe(pedido.getNumeroPedido()) + "</h3>"
                + "<table style='width:100%;border-collapse:collapse;margin-top:16px;'>"
                + "<thead>"
                + "<tr>"
                + "<th style='text-align:left;border-bottom:1px solid #ccc;padding:8px;'>Producto</th>"
                + "<th style='text-align:center;border-bottom:1px solid #ccc;padding:8px;'>Cant.</th>"
                + "<th style='text-align:right;border-bottom:1px solid #ccc;padding:8px;'>Precio</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>"
                + itemsHtml
                + "</tbody>"
                + "</table>"
                + "<p style='margin-top:16px;'><strong>Subtotal:</strong> S/ " + pedido.getSubtotal() + "</p>"
                + "<p><strong>Envío:</strong> S/ " + pedido.getCostoEnvio() + "</p>"
                + "<p><strong>Total:</strong> S/ " + pedido.getTotal() + "</p>"
                + "<p>Estado actual: <strong>" + safe(String.valueOf(pedido.getEstado())) + "</strong></p>"
                + "<p>Gracias por confiar en ElectroCYB ⚡</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String safe(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }
}