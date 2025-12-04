// ClienteRequest.java
package com.electrocyb.store.pedido.dto;

public record ClienteRequest(
        String nombre,
        String email,
        String telefono,
        String direccion,
        String referencia
) {}