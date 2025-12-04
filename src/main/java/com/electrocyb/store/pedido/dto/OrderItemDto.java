// OrderItemDto.java
package com.electrocyb.store.pedido.dto;

public record OrderItemDto(
        Long productoId,
        String nombre,
        String precio,
        String imagen,
        Integer cantidad
) {}