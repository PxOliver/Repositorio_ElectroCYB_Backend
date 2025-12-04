package com.electrocyb.store.producto;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductAdviceService {

    private final ProductoRepository productoRepository;

    // Umbrales de relevancia
    private static final int MIN_STRONG_SCORE_ABSOLUTE = 80;     // puntaje mínimo absoluto
    private static final double STRONG_SCORE_RATIO = 0.80;       // 80% del mejor
    private static final int MAX_PRODUCTS_RESPONSE = 4;          // máximo productos por respuesta
    private static final int CATALOG_LIMIT = 12;                 // tamaño máximo de catálogo general

    public ProductAdviceService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // ==========================================================
    // Tipos auxiliares
    // ==========================================================

    // Rango de precios
    public static class PriceRange {
        final BigDecimal min; // puede ser null
        final BigDecimal max; // puede ser null

        public PriceRange(BigDecimal min, BigDecimal max) {
            this.min = min;
            this.max = max;
        }

        public BigDecimal min() {
            return min;
        }

        public BigDecimal max() {
            return max;
        }
    }

    // Tipo de resultado de búsqueda
    public enum SearchType {
        NO_PRODUCTS_IN_DB,
        DB_NAME_MATCH,
        CATALOG_REQUEST,
        DIRECT_NAME_MATCH,
        TEXT_MATCH,
        FALLBACK
    }

    // Resultado estructurado para poder usarlo desde el ChatService + LLM
    public record ProductSearchResult(
            List<Producto> products,
            PriceRange priceRange,
            SearchType type,
            boolean catalogTruncated
    ) {}

    // Wrapper interno para puntaje
    private static class ScoredProduct {
        final Producto product;
        final int score;

        ScoredProduct(Producto product, int score) {
            this.product = product;
            this.score = score;
        }
    }

    // ==========================================================
    // Sinónimos, stopwords y tipos de producto
    // ==========================================================

    // Sinónimos básicos por dominio (normalizados, sin tildes)
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("foco", List.of("bombilla", "ampolleta", "foco", "luz", "led")),
            Map.entry("focos", List.of("foco", "bombilla", "ampolleta", "luz", "led")),
            Map.entry("lampara", List.of("lampara", "spot", "plafon", "panel")),
            Map.entry("lamparas", List.of("lampara", "spot", "plafon", "panel")),
            Map.entry("lampara techo", List.of("lampara techo", "plafon", "panel led")),
            Map.entry("reflector", List.of("reflector", "proyector")),
            Map.entry("sensor", List.of("sensor", "sensor de movimiento", "sensor movimiento", "sensor pir")),
            Map.entry("camara", List.of("camara", "camara de seguridad", "cctv")),
            Map.entry("tira", List.of("tira led", "cinta led", "strip led")),
            Map.entry("tira led", List.of("tira led", "cinta led", "strip led")),
            Map.entry("kit", List.of("kit", "kit solar", "kit de iluminacion")),
            Map.entry("kit solar", List.of("kit solar", "panel solar", "linterna solar"))
    );

    // Stopwords simples (en minúsculas, sin tilde)
    private static final Set<String> STOPWORDS = Set.of(
            "quiero", "busco", "necesito", "una", "un", "para", "que", "cual",
            "producto", "productos", "me", "recomiendame", "recomiendeme", "recomienda",
            "hasta", "maximo", "minimo", "entre", "desde", "soles", "s", "aprox",
            "al", "menos", "mas", "de", "a", "y", "como", "el", "la", "los", "las",
            "todos", "todas", "catalogo", "catalogo.", "catalogo,", "catálogo", "lista", "completa",
            "dame", "muestrame", "muéstrame", "ensename", "enséname", "quiero comprar",
            "precio", "barato", "barata", "caro", "cara", "tipo", "hay", "tienen",
            "por", "favor", "podrias", "podrías", "alrededor", "alrededor de",
            "aproximadamente", "cerca", "cerca de"
    );

    // Tipo de producto: lámparas / plafones / focos
    private static final Set<String> LAMP_KEYWORDS = Set.of(
            "lampara", "lamparas", "plafon", "plafones", "panel", "paneles",
            "foco", "focos", "spot", "dicroico", "dicroicos"
    );

    // Tipo de producto: tiras / cintas / mangueras
    private static final Set<String> STRIP_KEYWORDS = Set.of(
            "tira", "tiras", "tira led",
            "cinta", "cintas", "cinta led",
            "manguera", "mangueras", "manguera led",
            "neon", "neón", "cinta led neon"
    );

    // Palabras de ambientes (para sumar puntos si calza, pero no filtramos duro)
    private static final Set<String> ROOM_KEYWORDS = Set.of(
            "sala", "comedor", "dormitorio", "habitacion", "habitación",
            "cocina", "baño", "bano", "pasillo", "pasadizo"
    );

    private static final Set<String> SIGN_KEYWORDS = Set.of(
            "letrero", "letreros", "aviso", "avisos", "cartel", "carteles"
    );

    // ==========================================================
    // PÚBLICOS
    // ==========================================================

    /**
     * Búsqueda estructurada para que el ChatService pueda usarla.
     * Aquí se hace TODO el trabajo de entender el mensaje y mapearlo
     * a productos concretos del catálogo.
     */
    public ProductSearchResult findProductsForMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new ProductSearchResult(
                    List.of(),
                    null,
                    SearchType.FALLBACK,
                    false
            );
        }

        String original = userMessage.trim();
        String normalizedMsg = normalize(original);

        // 1) Traemos todos los productos (lee todo el catálogo)
        List<Producto> todos = productoRepository.findAll();
        if (todos.isEmpty()) {
            return new ProductSearchResult(
                    List.of(),
                    null,
                    SearchType.NO_PRODUCTS_IN_DB,
                    false
            );
        }

        // 2) BÚSQUEDA DIRECTA POR NOMBRE EN BD (match fuerte por nombre literal)
        List<Producto> fromDbByName = productoRepository
                .findTop5ByNombreContainingIgnoreCase(original);

        if (!fromDbByName.isEmpty()) {
            return new ProductSearchResult(
                    fromDbByName,
                    null,
                    SearchType.DB_NAME_MATCH,
                    false
            );
        }

        // 3) Catálogo completo / lista de productos (solo cuando piden "todo")
        if (isAskForAllProducts(normalizedMsg)) {
            List<Producto> limited = todos.stream()
                    .limit(CATALOG_LIMIT)
                    .collect(Collectors.toList());
            boolean truncated = todos.size() > CATALOG_LIMIT;

            return new ProductSearchResult(
                    limited,
                    null,
                    SearchType.CATALOG_REQUEST,
                    truncated
            );
        }

        // 4) Intentar match directo por nombre normalizado (en memoria)
        List<Producto> exactByName = todos.stream()
                .filter(p -> nameMatchesUserInput(p, normalizedMsg))
                .collect(Collectors.toList());

        if (!exactByName.isEmpty()) {
            List<Producto> limited = exactByName.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            return new ProductSearchResult(
                    limited,
                    null,
                    SearchType.DIRECT_NAME_MATCH,
                    false
            );
        }

        // 5) Detectar rango de precio (si existe) → soporta “alrededor de”, “menos de”, etc.
        PriceRange priceRange = extractPriceRange(normalizedMsg);

        // 6) Palabras clave + sinónimos
        List<String> expandedKeywords = extractKeywordsWithSynonyms(normalizedMsg);
        // tokens que el usuario escribió literalmente (sin sinónimos)
        List<String> coreTokens = extractCoreTokens(normalizedMsg);

        // 7) PRE-FILTRO desde la BD usando tus métodos existentes
        Set<Producto> preFilteredSet = new LinkedHashSet<>();
        preFilteredSet.addAll(productoRepository.findTop5ByDescripcionContainingIgnoreCase(original));
        preFilteredSet.addAll(productoRepository.findTop5ByCategoriaContainingIgnoreCase(original));

        try {
            List<Producto> wide = productoRepository
                    .findTop50ByNombreContainingIgnoreCaseOrCategoriaContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
                            original, original, original
                    );
            preFilteredSet.addAll(wide);
        } catch (Exception ignored) {
            // por si en algún entorno aún no existe el método
        }

        List<Producto> preFiltered = new ArrayList<>(preFilteredSet);
        if (preFiltered.size() < 5) {
            preFiltered = todos;
        }

        // 8) Score por relevancia de texto
        List<ScoredProduct> scored = preFiltered.stream()
                .map(p -> new ScoredProduct(p, scoreProduct(p, normalizedMsg, expandedKeywords, priceRange)))
                .filter(sp -> sp.score > 0)
                .sorted(Comparator.comparingInt((ScoredProduct sp) -> sp.score).reversed())
                .toList();

        // Si NADIE tiene score > 0, NO devolvemos productos al azar
        if (scored.isEmpty()) {
            return new ProductSearchResult(
                    List.of(),
                    priceRange,
                    SearchType.FALLBACK,
                    false
            );
        }

        // 9) Filtrar solo los matches FUERTES (top score)
        int maxScore = scored.get(0).score;
        int minScoreThreshold = Math.max(
                MIN_STRONG_SCORE_ABSOLUTE,
                (int) (maxScore * STRONG_SCORE_RATIO)
        );

        List<Producto> strongMatches = scored.stream()
                .filter(sp -> sp.score >= minScoreThreshold)
                .map(sp -> sp.product)
                // ⚠️ SOLO productos que coinciden con los tokens centrales del usuario
                .filter(p -> matchesCoreTokens(p, coreTokens))
                // ⚠️ Y que respetan la CLASE de producto (lámpara vs tira/manguera)
                .filter(p -> isProductClassCompatible(p, normalizedMsg))
                .collect(Collectors.toList());

        // Si los puntajes son muy parejos pero bajos, preferimos no inventar nada
        if (strongMatches.isEmpty()) {
            return new ProductSearchResult(
                    List.of(),
                    priceRange,
                    SearchType.FALLBACK,
                    false
            );
        }

        // 10) Filtro por stock (solo productos con stock o sin campo stock)
        List<Producto> byText = strongMatches.stream()
                .filter(this::hasStockOrNoStockField)
                .collect(Collectors.toList());

        // 11) Filtro de precio (estricto: si el usuario dio rango, se respeta SIEMPRE)
        List<Producto> filtered = byText;
        if (priceRange != null) {
            List<Producto> byPrice = byText.stream()
                    .filter(p -> isWithinPriceRange(p, priceRange))
                    .collect(Collectors.toList());

            // si el usuario pidió rango de precio y no hay nada → preferimos devolver nada
            if (byPrice.isEmpty()) {
                return new ProductSearchResult(
                        List.of(),
                        priceRange,
                        SearchType.FALLBACK,
                        false
                );
            }
            filtered = byPrice;
        }

        // 12) Si después de precio/stock se vacía, mejor pedir más detalle
        if (filtered.isEmpty()) {
            return new ProductSearchResult(
                    List.of(),
                    priceRange,
                    SearchType.FALLBACK,
                    false
            );
        }

        // 13) Top N (respuesta acotada, precisa)
        List<Producto> top = filtered.stream()
                .limit(MAX_PRODUCTS_RESPONSE)
                .collect(Collectors.toList());

        return new ProductSearchResult(
                top,
                priceRange,
                SearchType.TEXT_MATCH,
                false
        );
    }

    /**
     * Método de compatibilidad: genera la respuesta en TEXTO PLANO
     * con las viñetas "• [id] ..." para que el front las convierta en cards.
     */
    public String buildProductSuggestionText(String userMessage) {
        ProductSearchResult result = findProductsForMessage(userMessage);

        // No hay productos en DB
        if (result.type() == SearchType.NO_PRODUCTS_IN_DB) {
            return """
                    No encontré productos para lo que me indicas porque actualmente no hay productos registrados en la base de datos.

                    Revisa si la tabla 'productos' tiene datos cargados.
                    """;
        }

        // Si no hay nada utilizable, pedimos más info de forma inteligente
        if (result.products() == null || result.products().isEmpty()) {
            String precioInfo = buildPriceFilterText(result.priceRange());
            String extraPrecio = precioInfo.isBlank()
                    ? ""
                    : "\n\nAdemás, no encontré productos que cumplan exactamente con " + precioInfo + ".";
            return """
                    No estoy seguro de haber encontrado el producto exacto que necesitas 😅.

                    ¿Me puedes indicar un poco más de detalle? Por ejemplo:
                    - ¿Es para interior o exterior?
                    - ¿Para qué ambiente? (sala, dormitorio, fachada, jardín, etc.)
                    - ¿Presupuesto aproximado? (por ejemplo: hasta 50 soles)

                    Con eso puedo recomendarte mejores opciones de nuestro catálogo.
                    """ + extraPrecio;
        }

        String lista = result.products().stream()
                .map(this::formatProductLine)
                .collect(Collectors.joining("\n"));

        // Texto según tipo de búsqueda
        return switch (result.type()) {
            case DB_NAME_MATCH -> "Estos productos coinciden con el nombre que me indicaste:\n\n"
                    + lista
                    + "\n\nSi quieres más detalles de uno de ellos, haz clic en la tarjeta o dime el nombre.";
            case CATALOG_REQUEST -> {
                String extra = result.catalogTruncated()
                        ? "\n\n(Se muestran solo los primeros " + result.products().size() +
                        " productos del catálogo. Si buscas algo más específico, dime por ejemplo: 'foco led para sala', 'sensor de movimiento para pasadizo', etc.)"
                        : "\n\nSi quieres algo más específico, dime por ejemplo: 'foco led', 'sensor de movimiento', 'lámpara para sala', etc.";
                yield "Te muestro parte de nuestro catálogo de productos:\n\n"
                        + lista
                        + extra;
            }
            case DIRECT_NAME_MATCH -> "Estos productos coinciden directamente con el nombre que me indicaste:\n\n"
                    + lista
                    + "\n\nSi quieres más detalles de uno de ellos, dime el nombre o haz clic en la tarjeta.";
            case TEXT_MATCH, FALLBACK -> {
                String header = "Esto es lo que encontré según lo que me comentas";
                String priceText = buildPriceFilterText(result.priceRange());
                if (!priceText.isBlank()) {
                    header += " (considerando " + priceText + ")";
                }
                header += ":\n\n";
                String footer = "\n\nSi quieres más detalles de uno de ellos, dime el nombre o haz clic en la tarjeta.";
                yield header + lista + footer;
            }
            default -> "Esto es lo que encontré:\n\n" + lista;
        };
    }

    // ==========================================================
    // Helpers internos
    // ==========================================================

    /**
     * Solo devolvemos catálogo completo cuando el usuario pide explícitamente
     * "todo el catálogo", "todos los productos", "lista completa", etc.
     * Si solo dice "catálogo de focos", se va por búsqueda específica.
     */
    private boolean isAskForAllProducts(String normalizedMsg) {
        boolean mentionsCatalog = normalizedMsg.contains("catalogo")
                || normalizedMsg.contains("catálogo")
                || normalizedMsg.contains("lista de productos")
                || normalizedMsg.contains("lista completa");

        boolean wantsEverything = normalizedMsg.contains("todos los productos")
                || normalizedMsg.contains("todo el catalogo")
                || normalizedMsg.contains("todo el catálogo")
                || normalizedMsg.contains("todo tu catalogo")
                || normalizedMsg.contains("todo su catalogo")
                || normalizedMsg.contains("todo tu catálogo")
                || normalizedMsg.contains("ver todo")
                || normalizedMsg.contains("todo lo que tienes");

        return mentionsCatalog && wantsEverything;
    }

    private boolean nameMatchesUserInput(Producto p, String normalizedMsg) {
        if (p.getNombre() == null || p.getNombre().isBlank())
            return false;

        String nameNorm = normalize(p.getNombre());

        if (nameNorm.equals(normalizedMsg))
            return true;
        if (normalizedMsg.contains(nameNorm))
            return true;
        if (nameNorm.contains(normalizedMsg) && normalizedMsg.length() >= 4)
            return true;

        String[] nameParts = nameNorm.split("\\s+");
        int hits = 0;
        int totalWords = 0;
        for (String part : nameParts) {
            if (part.length() < 3)
                continue;
            totalWords++;
            if (normalizedMsg.contains(part)) {
                hits++;
            }
        }
        return totalWords > 0 && hits >= Math.max(1, totalWords / 2);
    }

    private String buildNormalizedProductText(Producto p) {
        StringBuilder sb = new StringBuilder();
        if (p.getNombre() != null)
            sb.append(p.getNombre()).append(" ");
        if (p.getCategoria() != null)
            sb.append(p.getCategoria()).append(" ");
        if (p.getDescripcion() != null)
            sb.append(p.getDescripcion()).append(" ");
        if (p.getCaracteristicas() != null && !p.getCaracteristicas().isEmpty()) {
            p.getCaracteristicas().forEach((k, v) -> {
                if (k != null)
                    sb.append(k).append(" ");
                if (v != null)
                    sb.append(v).append(" ");
            });
        }
        return normalize(sb.toString());
    }

    /**
     * Score de relevancia: texto + categoría + nombre + precio + ambiente + stock.
     */
    private int scoreProduct(Producto p, String normalizedMsg, List<String> keywords, PriceRange range) {
        int score = 0;

        if (nameMatchesUserInput(p, normalizedMsg)) {
            score += 200;
        }

        String productText = buildNormalizedProductText(p);
        String nombreNorm = p.getNombre() != null ? normalize(p.getNombre()) : "";
        String categoriaNorm = p.getCategoria() != null ? normalize(p.getCategoria()) : "";

        for (String kw : keywords) {
            if (kw.length() < 3)
                continue;
            if (productText.contains(kw)) {
                if (!nombreNorm.isBlank() && nombreNorm.contains(kw)) {
                    score += 40;
                } else if (!categoriaNorm.isBlank() && categoriaNorm.contains(kw)) {
                    score += 25;
                } else {
                    score += 10;
                }
            }
        }

        // Bonus si el producto tiene stock > 0
        if (hasStockOrNoStockField(p)) {
            score += 5;
        }

        // Bonus si el precio está cerca del centro del rango pedido
        if (range != null && p.getPrecio() != null && !p.getPrecio().isBlank()) {
            BigDecimal price = toBigDecimal(p.getPrecio());
            if (price != null) {
                BigDecimal mid = null;
                if (range.min() != null && range.max() != null) {
                    mid = range.min().add(range.max())
                            .divide(BigDecimal.valueOf(2), BigDecimal.ROUND_HALF_UP);
                } else if (range.min() != null) {
                    mid = range.min();
                } else if (range.max() != null) {
                    mid = range.max();
                }
                if (mid != null) {
                    BigDecimal diff = price.subtract(mid).abs();
                    if (mid.compareTo(BigDecimal.ZERO) > 0 &&
                            diff.compareTo(mid.multiply(BigDecimal.valueOf(0.2))) <= 0) {
                        score += 20; // muy cerca del precio esperado
                    }
                }
            }
        }

        // Bonus ambiente: si el usuario dice "sala" y el producto menciona "sala", etc.
        String msg = normalizedMsg;
        boolean msgRoom = containsAny(msg, ROOM_KEYWORDS);
        boolean msgSign = containsAny(msg, SIGN_KEYWORDS);

        boolean prodRoom = containsAny(productText, ROOM_KEYWORDS);
        boolean prodSign = containsAny(productText, SIGN_KEYWORDS);

        if (msgRoom && prodRoom) {
            score += 15; // mejor aún si dice "sala" y el producto también
        }
        if (msgRoom && prodSign) {
            score -= 10; // el usuario habla de sala y el producto habla de letreros → menos relevante
        }

        return score;
    }

    /**
     * Palabras clave + sinónimos (para score).
     */
    private List<String> extractKeywordsWithSynonyms(String normalizedMsg) {
        String[] parts = normalizedMsg.split("\\s+");
        Set<String> result = new LinkedHashSet<>();

        for (String raw : parts) {
            String token = raw.trim();
            if (token.length() < 3)
                continue;
            if (STOPWORDS.contains(token))
                continue;
            result.add(token);

            if (SYNONYMS.containsKey(token)) {
                SYNONYMS.get(token).forEach(s -> result.add(normalize(s)));
            }
        }

        if (result.isEmpty()) {
            result.add(normalizedMsg);
        }

        return new ArrayList<>(result);
    }

    /**
     * Tokens centrales tal cual los escribió el usuario (sin sinónimos).
     * Se usan para garantizar que el tipo de producto sea el correcto.
     */
    private List<String> extractCoreTokens(String normalizedMsg) {
        String[] parts = normalizedMsg.split("\\s+");
        List<String> core = new ArrayList<>();
        for (String raw : parts) {
            String token = raw.trim();
            if (token.length() < 3) continue;
            if (STOPWORDS.contains(token)) continue;
            core.add(token);
        }
        return core;
    }

    /**
     * Verifica que el producto contenga varios de los tokens que el usuario escribió
     * (en nombre o categoría).
     * - Si el usuario dio 1 token relevante → pedimos al menos 1 match.
     * - Si dio 2 o más tokens → pedimos al menos 2 matches para ser más estrictos.
     */
    private boolean matchesCoreTokens(Producto p, List<String> coreTokens) {
        if (coreTokens == null || coreTokens.isEmpty()) {
            return true; // no hay tipo específico, no filtramos
        }

        String nameCat = normalize(
                (p.getNombre() == null ? "" : p.getNombre()) + " " +
                (p.getCategoria() == null ? "" : p.getCategoria())
        );

        int hits = 0;
        for (String token : coreTokens) {
            if (token.length() < 3) continue;
            String t = normalize(token);
            // soporte simple para plural → singular (focos / foco, lamparas / lampara)
            String singular = t.endsWith("s") && t.length() > 3 ? t.substring(0, t.length() - 1) : t;

            if (nameCat.contains(t) || nameCat.contains(singular)) {
                hits++;
            }
        }

        if (coreTokens.size() >= 2) {
            return hits >= 2; // al menos 2 tokens relevantes deben aparecer
        }
        return hits >= 1;
    }

    /**
     * Forzamos coherencia de clase:
     * - Si el usuario pide "lámpara para sala" y NO menciona tira/manguera,
     *   no devolvemos cintas/mangueras puras.
     * - Si pide "tira led para letrero" y no menciona lámpara, no devolvemos lámparas.
     */
    private boolean isProductClassCompatible(Producto p, String normalizedMsg) {
        String msg = normalizedMsg;

        String productText = normalize(
                (p.getNombre() == null ? "" : p.getNombre()) + " " +
                (p.getCategoria() == null ? "" : p.getCategoria()) + " " +
                (p.getDescripcion() == null ? "" : p.getDescripcion())
        );

        boolean wantsLamp = containsAny(msg, LAMP_KEYWORDS);
        boolean wantsStrip = containsAny(msg, STRIP_KEYWORDS);

        boolean productIsLamp = containsAny(productText, LAMP_KEYWORDS);
        boolean productIsStrip = containsAny(productText, STRIP_KEYWORDS);

        if (wantsLamp && !wantsStrip) {
            // Usuario pidió lámpara → NO devolver solo cintas/mangueras
            if (productIsStrip && !productIsLamp) {
                return false;
            }
        }

        if (wantsStrip && !wantsLamp) {
            // Usuario pidió tira/manguera → NO devolver lámparas puras
            if (productIsLamp && !productIsStrip) {
                return false;
            }
        }

        return true;
    }

    private boolean containsAny(String text, Set<String> tokens) {
        if (text == null || text.isBlank() || tokens == null || tokens.isEmpty()) {
            return false;
        }
        for (String t : tokens) {
            if (text.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        if (input == null)
            return "";
        String lower = input.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    public String formatProductLine(Producto p) {
        String price = (p.getPrecio() != null && !p.getPrecio().isBlank())
                ? "S/ " + p.getPrecio()
                : "Precio no disponible";

        String desc = (p.getDescripcion() != null && !p.getDescripcion().isBlank())
                ? " — " + p.getDescripcion()
                : "";

        return "• [" + p.getId() + "] " + p.getNombre() + " — " + price + desc;
    }

    // ==========================================================
    // Precio
    // ==========================================================

    private PriceRange extractPriceRange(String normalizedMsg) {
        // 1) entre X y Y / de X a Y
        Pattern betweenPattern = Pattern.compile(
                "(?:entre|de)\\s+(\\d+(?:[.,]\\d+)?)\\s+(?:a|y)\\s+(\\d+(?:[.,]\\d+)?)"
        );
        Matcher mBetween = betweenPattern.matcher(normalizedMsg);
        if (mBetween.find()) {
            BigDecimal min = toBigDecimal(mBetween.group(1));
            BigDecimal max = toBigDecimal(mBetween.group(2));
            if (min != null && max != null) {
                if (min.compareTo(max) > 0) {
                    BigDecimal tmp = min;
                    min = max;
                    max = tmp;
                }
                return new PriceRange(min, max);
            }
        }

        // 2) hasta X / máximo X / no más de X / menos de X / no mayor a X
        Pattern maxPattern = Pattern.compile(
                "(?:hasta|maximo|como maximo|no mas de|menos de|no mayor a|no mayor de|por debajo de)\\s+(\\d+(?:[.,]\\d+)?)"
        );
        Matcher mMax = maxPattern.matcher(normalizedMsg);
        if (mMax.find()) {
            BigDecimal max = toBigDecimal(mMax.group(1));
            if (max != null) {
                return new PriceRange(null, max);
            }
        }

        // 3) desde X / mínimo X / al menos X / más de X / mayor a X
        Pattern minPattern = Pattern.compile(
                "(?:desde|a partir de|minimo|como minimo|al menos|mas de|mayor a|mayor de|por encima de)\\s+(\\d+(?:[.,]\\d+)?)"
        );
        Matcher mMin = minPattern.matcher(normalizedMsg);
        if (mMin.find()) {
            BigDecimal min = toBigDecimal(mMin.group(1));
            if (min != null) {
                return new PriceRange(min, null);
            }
        }

        // 4) alrededor de X / cerca de X / aproximadamente X
        Pattern approxPattern = Pattern.compile(
                "(?:alrededor de|cerca de|aproximadamente|aprox(?:\\.)?|por unos)\\s+(\\d+(?:[.,]\\d+)?)"
        );
        Matcher mApprox = approxPattern.matcher(normalizedMsg);
        if (mApprox.find()) {
            BigDecimal center = toBigDecimal(mApprox.group(1));
            if (center != null) {
                BigDecimal twentyPercent = center.multiply(BigDecimal.valueOf(0.2));
                BigDecimal min = center.subtract(twentyPercent);
                BigDecimal max = center.add(twentyPercent);
                if (min.compareTo(BigDecimal.ZERO) < 0)
                    min = BigDecimal.ZERO;
                return new PriceRange(min, max);
            }
        }

        return null;
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null)
            return null;
        try {
            String normalized = value.replace(",", ".").trim();
            return new BigDecimal(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isWithinPriceRange(Producto p, PriceRange range) {
        if (p.getPrecio() == null || p.getPrecio().isBlank()) {
            return false;
        }
        BigDecimal price = toBigDecimal(p.getPrecio());
        if (price == null)
            return false;

        if (range.min() != null && price.compareTo(range.min()) < 0) {
            return false;
        }
        if (range.max() != null && price.compareTo(range.max()) > 0) {
            return false;
        }
        return true;
    }

    private String buildPriceFilterText(PriceRange range) {
        if (range == null)
            return "";
        if (range.min() != null && range.max() != null) {
            return "precios entre S/ " + range.min() + " y S/ " + range.max();
        } else if (range.min() != null) {
            return "precios desde S/ " + range.min();
        } else if (range.max() != null) {
            return "precios hasta S/ " + range.max();
        }
        return "";
    }

    // ==========================================================
    // Stock
    // ==========================================================

    private boolean hasStockOrNoStockField(Producto p) {
        try {
            Integer stock = p.getStock();
            if (stock != null && stock <= 0) {
                return false;
            }
        } catch (Exception ignored) {
            // Si no hay campo stock o getter, no filtramos
        }
        return true;
    }
}