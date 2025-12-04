// ClienteDto.java
package com.electrocyb.store.pedido.dto;

public record ClienteDto(
        String nombre,
        String email,
        String telefono,
        String direccion,
        String referencia
) {}