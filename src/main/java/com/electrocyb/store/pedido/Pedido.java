package com.electrocyb.store.pedido;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código público tipo EC-000001
    @Column(unique = true, nullable = true, length = 50)
    private String numeroPedido;

    private Instant fecha;

    @Enumerated(EnumType.STRING)
    private OrderStatus estado;

    // 🔹 Nuevo: subtotal de productos
    private Double subtotal;

    // 🔹 Nuevo: costo de envío
    private Double costoEnvio;

    // 🔹 Total = subtotal + costoEnvio
    private Double total;

    // email del usuario autenticado que hizo el pedido
    private String emailUsuario;

    @Embedded
    private ClienteEmbeddable cliente;

    @OneToMany(mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pedido_historial",
            joinColumns = @JoinColumn(name = "pedido_id"))
    private List<HistorialEstadoEmbeddable> historialEstados = new ArrayList<>();
}