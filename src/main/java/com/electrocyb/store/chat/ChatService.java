package com.electrocyb.store.chat;

import com.electrocyb.store.producto.ProductAdviceService;
import com.electrocyb.store.producto.ProductAdviceService.ProductSearchResult;
import com.electrocyb.store.producto.ProductAdviceService.SearchType;
import com.electrocyb.store.producto.Producto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final WebClient webClient;
    private final String model;
    private final ProductAdviceService productAdviceService;

    public ChatService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.base.url}") String baseUrl,
            @Value("${openai.model}") String model,
            ProductAdviceService productAdviceService
    ) {
        this.model = model;
        this.productAdviceService = productAdviceService;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String getReply(ChatRequest request) {

        if ((request.message() == null || request.message().isBlank())
                && (request.history() == null || request.history().isEmpty())) {
            return "¿Me puedes indicar tu consulta? 😊";
        }

        String userMessage = request.message();
        String normalized = normalize(userMessage);

        // 1) FAQ de negocio (pagos, envíos, garantía) → respuesta fija
        String faqAnswer = handleBusinessFaq(normalized);
        if (faqAnswer != null) {
            return faqAnswer;
        }

        // 2) Detección de intención
        Intent intent = detectIntent(normalized);

        switch (intent) {
            case PRODUCT_INFO -> {
                // Búsqueda inteligente + texto de IA, pero LAS VIÑETAS LAS ARMAMOS NOSOTROS
                return replyWithProductIntelligence(userMessage);
            }
            case GENERAL -> {
                return callOpenAIGeneral(request);
            }
        }

        return callOpenAIGeneral(request);
    }

    // ==========================================================
    // PRODUCTOS INTELIGENTES
    // ==========================================================

    private String replyWithProductIntelligence(String userMessage) {
        ProductSearchResult result = productAdviceService.findProductsForMessage(userMessage);

        if (result.type() == SearchType.NO_PRODUCTS_IN_DB) {
            return """
                    No encontré productos registrados en el sistema por ahora.
                    
                    Por favor, revisa que la tabla de productos tenga datos cargados.
                    """;
        }

        List<Producto> productos = result.products();
        if (productos == null || productos.isEmpty()) {
            // aquí se usa el mensaje inteligente de follow-up
            return productAdviceService.buildProductSuggestionText(userMessage);
        }

        // 1) Intro corta generada por IA
        String intro = callOpenAIIntroForProducts(userMessage, result);

        if (intro == null || intro.isBlank()) {
            intro = "Esto es lo que te puedo recomendar de nuestro catálogo según lo que me comentas:";
        } else {
            intro = intro.trim();
        }

        // 2) Lista en formato controlado
        String lista = productos.stream()
                .map(productAdviceService::formatProductLine)
                .collect(Collectors.joining("\n"));

        String footer = "\n\nSi quieres más detalles de uno de ellos, dime el nombre o haz clic en la tarjeta.";

        return intro + "\n\n" + lista + footer;
    }

    private String callOpenAIIntroForProducts(String userMessage, ProductSearchResult result) {
        StringBuilder productsSummary = new StringBuilder();
        productsSummary.append("Lista de productos candidatos:\n");
        for (Producto p : result.products()) {
            productsSummary.append("- ID: ").append(p.getId())
                    .append(", Nombre: ").append(p.getNombre() == null ? "" : p.getNombre())
                    .append(", Categoria: ").append(p.getCategoria() == null ? "" : p.getCategoria())
                    .append(", Precio: ").append(p.getPrecio() == null ? "" : "S/ " + p.getPrecio())
                    .append(", Descripcion: ").append(p.getDescripcion() == null ? "" : p.getDescripcion())
                    .append("\n");
        }

        String priceFilterText = "";
        if (result.priceRange() != null) {
            if (result.priceRange().min() != null && result.priceRange().max() != null) {
                priceFilterText = "El cliente parece buscar precios entre S/ " + result.priceRange().min()
                        + " y S/ " + result.priceRange().max() + ".";
            } else if (result.priceRange().min() != null) {
                priceFilterText = "El cliente parece buscar precios desde S/ " + result.priceRange().min() + ".";
            } else if (result.priceRange().max() != null) {
                priceFilterText = "El cliente parece buscar precios hasta S/ " + result.priceRange().max() + ".";
            }
        }

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", """
                        Eres el asistente de ElectroCYB. Respondes siempre en español (Perú).
                        Te daré una lista de productos de iluminación ya filtrados desde la base de datos.
                        
                        TU TAREA:
                        - Escribir SOLO un párrafo corto (1 o 2 frases) explicando por qué esos productos son adecuados
                          para lo que pide el cliente.
                        - NO debes enumerar los productos ni escribir viñetas.
                        - NO inventes productos ni precios.
                        - No uses negritas ni Markdown, solo texto plano.
                        """
        ));

        StringBuilder userContent = new StringBuilder();
        userContent.append("Mensaje del cliente: ").append(userMessage).append("\n\n");
        if (!priceFilterText.isBlank()) {
            userContent.append(priceFilterText).append("\n\n");
        }
        userContent.append(productsSummary);

        messages.add(Map.of(
                "role", "user",
                "content", userContent.toString()
        ));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.3,
                "max_tokens", 120
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(Map.of());
                })
                .block();

        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty())
                return null;

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            return content != null ? content.toString() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==========================================================
    // LLM general (no productos)
    // ==========================================================

    private String callOpenAIGeneral(ChatRequest request) {

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", buildMessages(request),
                "temperature", 0.3,
                "max_tokens", 400
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(Map.of());
                })
                .block();

        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty())
                return "Lo siento, no pude generar una respuesta.";

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            return content != null ? content.toString() : "Lo siento, no pude responder.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Ocurrió un error al procesar tu consulta.";
        }
    }

    private List<Map<String, String>> buildMessages(ChatRequest request) {

        List<Map<String, String>> result = new ArrayList<>();

        result.add(Map.of(
                "role", "system",
                "content", """
                        Eres el asistente de ElectroCYB. Respondes siempre en español (Perú).
                        Puedes ayudar sobre productos de iluminación, catálogos, recomendaciones, compatibilidad y compras.
                        
                        IMPORTANTE:
                        - NO inventes métodos de pago, formas de envío ni garantías.
                        - Si el usuario pregunta por métodos de pago, envíos o garantía, y no tienes la información,
                          indica que esa información depende de la tienda y que el cliente debe confirmarla por los canales oficiales.
                        Sé breve, amable y útil.
                        """
        ));

        if (request.history() != null) {
            List<Map<String, String>> history = request.history().stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();
            result.addAll(limitHistory(history, 12));
        }

        result.add(Map.of("role", "user", "content", request.message()));

        return result;
    }

    private List<Map<String, String>> limitHistory(List<Map<String, String>> history, int max) {
        if (history.size() <= max) return history;
        return history.subList(history.size() - max, history.size());
    }

    // ==========================================================
    // INTENTOS y FAQ
    // ==========================================================

    private Intent detectIntent(String normalizedMsg) {
        String t = normalizedMsg;

        // Claramente relacionado a productos / precios / catálogo
        if (t.contains("recomiendame") || t.contains("recomienda")
                || t.contains("busco") || t.contains("quiero comprar")
                || t.contains("me sirve") || t.contains("que producto")
                || t.contains("cual producto") || t.contains("producto")
                || t.contains("foco") || t.contains("focos")
                || t.contains("lampara") || t.contains("lamparas")
                || t.contains("led") || t.contains("sensor")
                || t.contains("camara") || t.contains("camaras")
                || t.contains("seguridad") || t.contains("reflector")
                || t.contains("bombilla") || t.contains("spot")
                || t.contains("dicroico") || t.contains("kit solar")
                || t.contains("tira led") || t.contains("tira") || t.contains("cinta led")
                || t.contains("precio") || t.contains("cuanto cuesta")
                || t.contains("cuánto cuesta") || t.contains("cuanto vale")
                || t.contains("cuánto vale") || t.contains("vale")
                || t.contains("catalogo") || t.contains("catálogo")) {
            return Intent.PRODUCT_INFO;
        }

        return Intent.GENERAL;
    }

    private String handleBusinessFaq(String normalizedMsg) {
        if (isPaymentQuestion(normalizedMsg)) {
            return """
                    Actualmente aceptamos pagos únicamente por Yape.
                    
                    Al coordinar tu pedido te enviaremos el número o el código QR de Yape
                    para que puedas realizar el pago de forma rápida y segura.
                    """.trim();
        }

        if (isShippingQuestion(normalizedMsg)) {
            return """
                    Estos son nuestros métodos de entrega:
                    
                    - Recojo en tienda (Lima, Perú), sin costo adicional.
                    - Entrega a domicilio en Lima Metropolitana, con costo según distrito.
                    - Envío a otros departamentos del Perú, con costo según el departamento.
                    
                    Si me indicas tu distrito o ciudad, puedo orientarte mejor.
                    """.trim();
        }

        if (isWarrantyQuestion(normalizedMsg)) {
            return """
                    La garantía depende del producto específico.
                    
                    En general, al ser productos de iluminación, manejamos un aproximado
                    de 6 meses de garantía, pero puede variar según el tipo de producto
                    y el proveedor.
                    """.trim();
        }

        return null;
    }

    private boolean isPaymentQuestion(String normalizedMsg) {
        return (normalizedMsg.contains("metodo de pago") ||
                normalizedMsg.contains("metodos de pago") ||
                normalizedMsg.contains("forma de pago") ||
                normalizedMsg.contains("formas de pago") ||
                normalizedMsg.contains("como pago") ||
                normalizedMsg.contains("como puedo pagar") ||
                (normalizedMsg.contains("pago") && normalizedMsg.contains("acept")));
    }

    private boolean isShippingQuestion(String normalizedMsg) {
        return (normalizedMsg.contains("envio") ||
                normalizedMsg.contains("envios") ||
                normalizedMsg.contains("entrega") ||
                normalizedMsg.contains("delivery") ||
                normalizedMsg.contains("recojo en tienda") ||
                normalizedMsg.contains("recoger en tienda") ||
                normalizedMsg.contains("envian a") ||
                normalizedMsg.contains("envio a domicilio") ||
                normalizedMsg.contains("envias a") ||
                normalizedMsg.contains("costo de envio"));
    }

    private boolean isWarrantyQuestion(String normalizedMsg) {
        return (normalizedMsg.contains("garantia") ||
                normalizedMsg.contains("garantía") ||
                (normalizedMsg.contains("cambio") && normalizedMsg.contains("producto")) ||
                normalizedMsg.contains("devolucion") ||
                normalizedMsg.contains("devolución"));
    }

    private String normalize(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private enum Intent {
        GENERAL,
        PRODUCT_INFO
    }
}