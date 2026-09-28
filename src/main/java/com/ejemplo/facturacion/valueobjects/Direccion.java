package com.ejemplo.facturacion.valueobjects;

public class Direccion {
    private String pais;
    private String estado;
    private String municipio;
    private String ciudad;
    private String calle;
    private String numero;
    private String telefono;
    private String codigoPostal;

    public Direccion() {
    }
  
    public Direccion(String pais, String estado, String municipio, String ciudad, String calle, String numero,
            String telefono, String codigoPostal) {
        this.pais = pais;
        this.estado = estado;
        this.municipio = municipio;
        this.ciudad = ciudad;
        this.calle = calle;
        this.numero = numero;
        this.telefono = telefono;
        this.codigoPostal = codigoPostal;
    }

    public String getPais() {
        return pais;
    }
    public void setPais(String pais) {
        this.pais = pais;
    }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    public String getCiudad() {
        return ciudad;
    }
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }
    public String getCalle() {
        return calle;
    }
    public void setCalle(String calle) {
        this.calle = calle;
    }
    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        this.numero = numero;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    public String getCodigoPostal() {
        return codigoPostal;
    }
    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }
}
