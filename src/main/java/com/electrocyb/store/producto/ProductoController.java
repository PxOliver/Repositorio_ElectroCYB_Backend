package com.electrocyb.store.producto;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // GET /api/productos  → listado catálogo / admin
    @GetMapping
    public List<Producto> listar() {
        return service.listarTodos();
    }

    // GET /api/productos/{id}  → detalle
    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    // GET /api/productos/categoria/{categoria}
    @GetMapping("/categoria/{categoria}")
    public List<Producto> porCategoria(@PathVariable String categoria) {
        return service.buscarPorCategoria(categoria);
    }

    // POST /api/productos  → crear (ADMIN)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@RequestBody Producto p) {
        return service.crear(p);
    }

    // PUT /api/productos/{id}  → actualizar (ADMIN)
    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @RequestBody Producto p) {
        return service.actualizar(id, p);
    }

    // DELETE /api/productos/{id}  → eliminar (ADMIN)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}