package com.electrocyb.store.pedido;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ClienteEmbeddable {

    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String referencia;
}