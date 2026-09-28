package com.ejemplo.facturacion.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ejemplo.facturacion.services.FacturaService;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

/**
 * Version 2 del API de facturacion, de naturaleza asincrona.
 *
 * El POST no espera a que la factura quede calculada: acusa recibo con un
 * 202 Accepted e informa en los encabezados donde consultar el resultado.
 * El GET reporta el avance mediante el codigo de estado.
 */
@RestController
public class FacturacionV2Controller {

    private static final String RUTA_CONSULTA = "/v2/factura/";

    @Autowired
    FacturaService facturaService;

    /**
     * Recibe la orden, arranca la generacion de la factura en segundo plano y
     * responde de inmediato.
     *
     * Responde 202 Accepted, que es el codigo destinado a las peticiones que se
     * aceptaron pero cuyo procesamiento aun no concluye. Los encabezados llevan:
     * - Location:    la URL exacta donde se consultara la factura
     * - Id-Factura:  el identificador asignado
     * - Retry-After: los segundos sugeridos antes de volver a consultar
     */
    @PostMapping("/v2/factura")
    public ResponseEntity<String> calcularFactura(@RequestBody Orden orden) throws InterruptedException {
        String idFactura = facturaService.iniciarFacturaAsincrona(orden);

        MultiValueMap<String, String> encabezados = new LinkedMultiValueMap<>();
        encabezados.add(HttpHeaders.LOCATION, RUTA_CONSULTA + idFactura);
        encabezados.add(HttpHeaders.RETRY_AFTER, "5");
        encabezados.add("Id-Factura", idFactura);

        return new ResponseEntity<>(idFactura, encabezados, HttpStatus.ACCEPTED);
    }

    /**
     * Consulta el estado de una factura solicitada previamente.
     *
     * - 200 OK        la factura ya fue generada y viaja en el cuerpo
     * - 204 No Content el identificador es valido pero la factura sigue generandose
     * - 404 Not Found  el identificador no corresponde a ninguna factura
     */
    @GetMapping("/v2/factura/{idFactura}")
    public ResponseEntity<Factura> buscarFactura(@PathVariable String idFactura) {
        Optional<Factura> factura = facturaService.obtenerFacturaAsincrona(idFactura);

        if (factura == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (factura.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(factura.get(), HttpStatus.OK);
    }
}
