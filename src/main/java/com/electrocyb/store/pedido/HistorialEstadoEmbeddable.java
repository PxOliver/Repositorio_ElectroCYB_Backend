package com.electrocyb.store.pedido;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Embeddable
@Getter
@Setter
public class HistorialEstadoEmbeddable {

    @Enumerated(EnumType.STRING)
    private OrderStatus estado;

    private Instant fecha;

    private String descripcion;
}