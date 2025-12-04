package com.electrocyb.store.pedido;

import com.electrocyb.store.pedido.dto.CambiarEstadoRequest;
import com.electrocyb.store.pedido.dto.CreateOrderRequest;
import com.electrocyb.store.pedido.dto.OrderResponseDto;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // 🔹 Crear pedido
    @PostMapping
    public OrderResponseDto crearPedido(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String emailUsuario = null;
        if (userDetails != null) {
            emailUsuario = userDetails.getUsername();
        } else if (request.cliente() != null) {
            emailUsuario = request.cliente().email();
        }

        return pedidoService.crearPedido(request, emailUsuario);
    }

    // 🔹 Obtener pedido por número (para tracking)
    @GetMapping("/{numeroPedido}")
    public OrderResponseDto obtenerPorNumero(@PathVariable String numeroPedido) {
        return pedidoService.obtenerPorNumeroPedido(numeroPedido);
    }

    @GetMapping("/mios")
    public List<OrderResponseDto> misPedidos(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        String email = userDetails.getUsername();
        return pedidoService.listarPorEmailUsuario(email);
    }

    // 🔹 Listar pedidos (para panel admin / general)
    @GetMapping
    public List<OrderResponseDto> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    // 🔒 Solo ADMIN (según SecurityConfig) → AdminPanel
    @GetMapping("/admin/lista")
    public List<OrderResponseDto> listarTodos() {
        return pedidoService.listarTodos();
    }

    // 🔹 Cambiar estado de un pedido
    @PatchMapping("/{numeroPedido}/estado")
    public OrderResponseDto cambiarEstado(
            @PathVariable String numeroPedido,
            @RequestBody CambiarEstadoRequest request
    ) {
        return pedidoService.cambiarEstado(
                numeroPedido,
                request.nuevoEstado(),
                request.descripcion()
        );
    }
}