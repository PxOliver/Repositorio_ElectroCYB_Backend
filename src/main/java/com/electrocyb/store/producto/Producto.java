package com.electrocyb.store.producto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "productos")
@Getter
@Setter
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String categoria;

    @Column(length = 1000)
    private String descripcion;

    // Ruta o URL relativa de la imagen (ej: "/uploads/productos/led-1.jpg")
    private String imagen;

    // Usa el tipo que ya tengas en tu entidad (BigDecimal, Double, etc.)
    private String precio;

    private Integer stock;

    // 👇 Mapeo de la tabla producto_caracteristicas
    @ElementCollection
    @CollectionTable(
        name = "producto_caracteristicas",
        joinColumns = @JoinColumn(name = "producto_id")
    )
    @MapKeyColumn(name = "nombre") // columna para la clave (ej. "potencia")
    @Column(name = "valor")        // columna para el valor (ej. "12W")
    private Map<String, String> caracteristicas = new HashMap<>();
}
