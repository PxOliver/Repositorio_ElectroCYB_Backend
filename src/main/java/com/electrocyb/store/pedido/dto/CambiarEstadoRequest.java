package com.electrocyb.store.pedido.dto;

import com.electrocyb.store.pedido.OrderStatus;

public record CambiarEstadoRequest(
        OrderStatus nuevoEstado,
        String descripcion
) {}
