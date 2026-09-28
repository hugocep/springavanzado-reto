package com.ejemplo.facturacion.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Usuario {
    @Id
    private int id;
    private String nombreUsuario;
    private String contraseña;
    private String autoridad;

    public int getId() {return id;}
    public void setId(int id) {this.id = id;}
    public String getNombreUsuario() {return nombreUsuario;}
    public void setNombreUsuario(String nombreUsuario) {this.nombreUsuario = nombreUsuario;}
    public String getContraseña() {return contraseña;}
    public void setContraseña(String contraseña) {this.contraseña = contraseña;}
    public String getAutoridad() {return autoridad;}
    public void setAutoridad(String autoridad) {this.autoridad = autoridad;}  
}

