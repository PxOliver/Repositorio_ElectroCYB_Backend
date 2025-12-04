package com.electrocyb.store.producto;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {

    private final ProductoRepository repo;

    public ProductoService(ProductoRepository repo) {
        this.repo = repo;
    }

    public List<Producto> listarTodos() {
        return repo.findAll();
    }

    public Producto obtenerPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    public List<Producto> buscarPorCategoria(String categoria) {
        return repo.findByCategoriaIgnoreCase(categoria);
    }

    public Producto crear(Producto p) {
        // si viene null, lo inicializamos para evitar NPE
        if (p.getCaracteristicas() == null) {
            p.setCaracteristicas(new HashMap<>());
        }
        return repo.save(p);
    }

    public Producto actualizar(Long id, Producto datos) {
        Producto existente = obtenerPorId(id);

        existente.setNombre(datos.getNombre());
        existente.setPrecio(datos.getPrecio());
        existente.setDescripcion(datos.getDescripcion());
        existente.setCategoria(datos.getCategoria());
        existente.setImagen(datos.getImagen());
        existente.setStock(datos.getStock());

        // 👇 actualizar características
        Map<String, String> nuevas = datos.getCaracteristicas();

        if (nuevas == null || nuevas.isEmpty()) {
            // si viene vacío, limpiamos la colección
            existente.getCaracteristicas().clear();
        } else {
            // limpiamos y volvemos a poner todo
            existente.getCaracteristicas().clear();
            existente.getCaracteristicas().putAll(nuevas);
        }

        return repo.save(existente);
    }

    public void eliminar(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Producto no existe");
        }
        repo.deleteById(id);
    }
}