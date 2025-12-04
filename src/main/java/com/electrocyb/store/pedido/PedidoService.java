package com.electrocyb.store.pedido;

import com.electrocyb.store.email.EmailService;
import com.electrocyb.store.pedido.dto.*;
import com.electrocyb.store.producto.Producto;
import com.electrocyb.store.producto.ProductoRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EmailService emailService;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         EmailService emailService,
                         ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.emailService = emailService;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public OrderResponseDto crearPedido(CreateOrderRequest request, String emailUsuario) {

        Pedido pedido = new Pedido();
        pedido.setFecha(Instant.now());
        pedido.setEstado(OrderStatus.RECIBIDO);
        pedido.setEmailUsuario(emailUsuario);

        // Cliente
        ClienteEmbeddable c = new ClienteEmbeddable();
        c.setNombre(request.cliente().nombre());
        c.setEmail(request.cliente().email());
        c.setTelefono(request.cliente().telefono());
        c.setDireccion(request.cliente().direccion());
        c.setReferencia(request.cliente().referencia());
        pedido.setCliente(c);

        // Items
        double subtotal = 0.0;

        for (OrderItemRequest itemReq : request.items()) {

            // 🔥 DESCONTAR STOCK ANTES DE GUARDAR EL PEDIDO
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemReq.nombre()));

            if (producto.getStock() < itemReq.cantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre());
            }

            producto.setStock(producto.getStock() - itemReq.cantidad());
            productoRepository.save(producto);

            // Crear item del pedido
            OrderItem item = new OrderItem();
            item.setProductoId(itemReq.productoId());
            item.setNombre(itemReq.nombre());
            item.setPrecio(itemReq.precio());
            item.setImagen(itemReq.imagen());
            item.setCantidad(itemReq.cantidad());
            item.setPedido(pedido);
            pedido.getItems().add(item);

            try {
                double precio = Double.parseDouble(itemReq.precio());
                subtotal += precio * itemReq.cantidad();
            } catch (NumberFormatException ignored) {}
        }

        // Subtotal
        pedido.setSubtotal(subtotal);

        // Costo de envío
        double costoEnvio = calcularCostoEnvio(c);
        pedido.setCostoEnvio(costoEnvio);

        // Total
        pedido.setTotal(subtotal + costoEnvio);

        // Historial
        HistorialEstadoEmbeddable h = new HistorialEstadoEmbeddable();
        h.setEstado(OrderStatus.RECIBIDO);
        h.setFecha(Instant.now());
        h.setDescripcion("Pedido recibido.");
        pedido.getHistorialEstados().add(h);

        // Guardar pedido
        pedido = pedidoRepository.save(pedido);

        // Generar número EC-000001
        pedido.setNumeroPedido(generarCodigoPedido(pedido.getId()));
        pedido = pedidoRepository.save(pedido);

        return mapToDto(pedido);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto obtenerPorNumeroPedido(String numeroPedido) {
        Pedido pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        return mapToDto(pedido);
    }

    private String generarCodigoPedido(Long id) {
        return "EC-" + String.format("%06d", id);
    }

    private double calcularCostoEnvio(ClienteEmbeddable cliente) {
        if (cliente == null) return 12.0;

        String base = "";
        if (cliente.getDireccion() != null) base += cliente.getDireccion() + " ";
        if (cliente.getReferencia() != null) base += cliente.getReferencia();

        String text = base.toLowerCase();

        if (containsAny(text, "cercado de lima", "lima", "breña", "pueblo libre",
                "jesús maría", "jesus maria", "lince", "la victoria",
                "san miguel", "magdalena del mar", "magdalena")) return 8.0;

        if (containsAny(text, "miraflores", "san isidro", "surquillo",
                "barranco", "san borja")) return 10.0;

        if (containsAny(text, "santiago de surco", "surco", "chorrillos", "la molina",
                "san luis", "rímac", "rimac")) return 12.0;

        if (containsAny(text, "san juan de lurigancho", "san juan de miraflores",
                "villa el salvador", "villa maría del triunfo", "villa maria del triunfo",
                "comas", "independencia", "los olivos", "san martín de porres",
                "san martin de porres", "ate", "el agustino", "santa anita",
                "carabayllo")) return 14.0;

        if (containsAny(text, "callao", "bellavista", "la perla",
                "la punta", "carmen de la legua")) return 15.0;

        if (containsAny(text, "tumbes", "piura", "lambayeque", "chiclayo",
                "la libertad", "trujillo", "ancash", "chimbote",
                "ica", "pisco", "chincha", "moquegua",
                "tacna", "arequipa")) return 20.0;

        if (containsAny(text, "cajamarca", "amazonas", "san martín", "san martin",
                "loreto", "huánuco", "huanuco", "pasco",
                "junín", "junin", "huancavelica", "ayacucho",
                "cusco", "puno", "apurímac", "apurimac",
                "madre de dios", "ucayali", "huancayo",
                "juliaca", "tarapoto")) return 24.0;

        return 12.0;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) if (text.contains(token.toLowerCase())) return true;
        return false;
    }

    private OrderResponseDto mapToDto(Pedido pedido) {

        List<OrderItemDto> itemDtos = pedido.getItems() == null
                ? List.of()
                : pedido.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProductoId(),
                        i.getNombre(),
                        i.getPrecio(),
                        i.getImagen(),
                        i.getCantidad()
                )).toList();

        List<HistorialEstadoDto> historialDtos = pedido.getHistorialEstados() == null
                ? List.of()
                : pedido.getHistorialEstados().stream()
                .map(h -> new HistorialEstadoDto(
                        h.getEstado(),
                        h.getFecha(),
                        h.getDescripcion()
                )).toList();

        ClienteEmbeddable c = pedido.getCliente();
        ClienteDto clienteDto = new ClienteDto(
                c != null ? c.getNombre() : null,
                c != null ? c.getEmail() : null,
                c != null ? c.getTelefono() : null,
                c != null ? c.getDireccion() : null,
                c != null ? c.getReferencia() : null
        );

        return new OrderResponseDto(
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getSubtotal(),
                pedido.getCostoEnvio(),
                pedido.getTotal(),
                clienteDto,
                itemDtos,
                historialDtos
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listarPedidos() {
        return pedidoRepository.findAllByOrderByFechaDesc()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public OrderResponseDto cambiarEstado(
            String numeroPedido,
            OrderStatus nuevoEstado,
            String descripcion) {

        Pedido pedido = pedidoRepository.findByNumeroPedido(numeroPedido)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setEstado(nuevoEstado);

        HistorialEstadoEmbeddable h = new HistorialEstadoEmbeddable();
        h.setEstado(nuevoEstado);
        h.setFecha(Instant.now());
        h.setDescripcion(
                (descripcion != null && !descripcion.isBlank())
                        ? descripcion
                        : "Estado actualizado a " + nuevoEstado.name()
        );

        pedido.getHistorialEstados().add(h);

        pedido = pedidoRepository.save(pedido);

        if (nuevoEstado == OrderStatus.ENTREGADO) {
            try {
                emailService.sendOrderDeliveredEmail(pedido);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        return mapToDto(pedido);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> listarPorEmailUsuario(String emailUsuario) {
        return pedidoRepository.findByEmailUsuarioOrderByFechaDesc(emailUsuario)
                .stream()
                .map(this::mapToDto)
                .toList();
    }
}
