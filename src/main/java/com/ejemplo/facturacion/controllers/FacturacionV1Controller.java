package com.ejemplo.facturacion.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ejemplo.facturacion.services.FacturaService;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

@RestController
public class FacturacionV1Controller {
    @Autowired FacturaService facturaService;

    @PostMapping("/v1/factura")
    public ResponseEntity<Factura> calcularFactura(@RequestBody Orden orden) throws InterruptedException {
        Factura factura = facturaService.generarFactura(orden);
        return new ResponseEntity<>(factura, HttpStatus.CREATED);
    }
}
