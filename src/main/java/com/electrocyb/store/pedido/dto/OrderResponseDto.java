package com.electrocyb.store.pedido.dto;

import com.electrocyb.store.pedido.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String numeroPedido,
        Instant fecha,
        OrderStatus estado,
        Double subtotal,
        Double costoEnvio,
        Double total,
        ClienteDto cliente,
        List<OrderItemDto> items,
        List<HistorialEstadoDto> historialEstados
) {}