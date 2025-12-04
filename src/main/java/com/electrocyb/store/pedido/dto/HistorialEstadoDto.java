// HistorialEstadoDto.java
package com.electrocyb.store.pedido.dto;

import com.electrocyb.store.pedido.OrderStatus;

import java.time.Instant;

public record HistorialEstadoDto(
        OrderStatus estado,
        Instant fecha,
        String descripcion
) {}