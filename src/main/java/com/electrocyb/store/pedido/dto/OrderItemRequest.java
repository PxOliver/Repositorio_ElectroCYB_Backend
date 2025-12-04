// OrderItemRequest.java
package com.electrocyb.store.pedido.dto;

public record OrderItemRequest(
        Long productoId,
        String nombre,
        String precio,
        String imagen,
        Integer cantidad
) {}