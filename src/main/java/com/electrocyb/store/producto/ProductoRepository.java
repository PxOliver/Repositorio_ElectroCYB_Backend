package com.electrocyb.store.producto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCategoriaIgnoreCase(String categoria);

    List<Producto> findTop5ByNombreContainingIgnoreCase(String nombre);

    List<Producto> findTop5ByDescripcionContainingIgnoreCase(String descripcion);

    List<Producto> findTop5ByCategoriaContainingIgnoreCase(String categoria);

    // 🔥 Búsqueda amplia para el motor "inteligente"
    List<Producto> findTop50ByNombreContainingIgnoreCaseOrCategoriaContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
            String nombre,
            String categoria,
            String descripcion
    );
}