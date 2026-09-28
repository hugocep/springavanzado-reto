package com.ejemplo.facturacion.valueobjects;

import java.math.BigDecimal;

public class Articulo {
    private String producto;
    private long cantidad;
    private BigDecimal precioUnitario;

    public Articulo() {
    }

    public Articulo(String producto, long cantidad, BigDecimal precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getProducto() {
        return producto;
    }
    public void setProducto(String producto) {
        this.producto = producto;
    }
    public long getCantidad() {
        return cantidad;
    }
    public void setCantidad(long cantidad) {
        this.cantidad = cantidad;
    }
    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }


}
