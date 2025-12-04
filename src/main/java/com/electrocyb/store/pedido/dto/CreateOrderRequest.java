package com.electrocyb.store.pedido.dto;

import java.util.List;

public record CreateOrderRequest(
        ClienteRequest cliente,
        List<OrderItemRequest> items,
        String metodoPago,
        String metodoEntrega, // 👈 NECESARIO para recojo en tienda = 0 envío
        String notas
) {}