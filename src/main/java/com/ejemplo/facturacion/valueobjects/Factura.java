package com.ejemplo.facturacion.valueobjects;

import java.math.BigDecimal;
import java.util.List;

public class Factura {
    private String id;
    private List<Articulo> articulos;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;

    public Factura() {}

    public Factura(String id, List<Articulo> articulos, BigDecimal subtotal, BigDecimal iva, BigDecimal total) {
        this.id = id;
        this.articulos = articulos;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
    }

    public List<Articulo> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<Articulo> articulos) {
        this.articulos = articulos;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public void setIva(BigDecimal iva) {
        this.iva = iva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }  
}
