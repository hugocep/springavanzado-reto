package com.ejemplo.facturacion.valueobjects;

import java.util.List;

public class Orden {
    private Long id;
    private String usuario;
    private Direccion direccionCliente;
    private List<Articulo> articulos;

    public Orden() {
    }

    public Orden(Long id, String usuario, Direccion direccionEnvio, List<Articulo> articulos) {
        this.id = id;
        this.usuario = usuario;
        this.direccionCliente = direccionEnvio;
        this.articulos = articulos;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }    
    public String getUsuario() {
        return usuario;
    }
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    public Direccion getDireccionCliente() {
        return direccionCliente;
    }
    public void setDireccionCliente(Direccion direccionEnvio) {
        this.direccionCliente = direccionEnvio;
    }
    public List<Articulo> getArticulos() {
        return articulos;
    }
    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }
}
