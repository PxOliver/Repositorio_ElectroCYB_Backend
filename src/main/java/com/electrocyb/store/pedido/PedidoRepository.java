package com.electrocyb.store.pedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroPedido(String numeroPedido);

    // 🔹 Listar pedidos ordenados por fecha (más recientes primero)
    List<Pedido> findAllByOrderByFechaDesc();

    List<Pedido> findByEmailUsuarioOrderByFechaDesc(String emailUsuario);
}