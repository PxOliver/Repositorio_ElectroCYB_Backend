// CreateOrderRequest.java
package com.electrocyb.store.pedido.dto;

import java.util.List;

public record CreateOrderRequest(
        ClienteRequest cliente,
        java.util.List<OrderItemRequest> items,
        String metodoPago,
        String notas
) {}