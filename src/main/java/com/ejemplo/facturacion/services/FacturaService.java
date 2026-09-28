package com.ejemplo.facturacion.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ejemplo.facturacion.valueobjects.Articulo;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

/**
 * Servicio de generacion de facturas.
 *
 * Ofrece dos esquemas de trabajo:
 *
 * 1. Sincrono (v1): el metodo generarFactura bloquea al hilo que atiende la
 *    peticion HTTP hasta que la factura queda calculada. Su comportamiento
 *    permanece intacto respecto a la version original.
 *
 * 2. Asincrono (v2): iniciarFacturaAsincrona registra la factura como pendiente,
 *    delega el calculo a un hilo del pool de tareas mediante crearFacturaAsincrona
 *    y devuelve de inmediato el identificador. El cliente consulta despues el
 *    resultado con obtenerFacturaAsincrona.
 */
@Service
public class FacturaService {

    private static final Logger LOG = LoggerFactory.getLogger(FacturaService.class);

    /**
     * Almacen en memoria de facturas.
     *
     * Estados posibles de una entrada:
     * - la llave no existe           -> el identificador es desconocido (HTTP 404)
     * - la llave existe y esta vacia -> la factura se esta generando    (HTTP 204)
     * - la llave existe con valor    -> la factura esta lista           (HTTP 200)
     *
     * Se usa ConcurrentHashMap porque el mapa es escrito por los hilos del pool
     * asincrono y leido al mismo tiempo por los hilos que atienden las peticiones.
     */
    private Map<String, Optional<Factura>> facturas = new ConcurrentHashMap<>();

    /**
     * Auto-referencia al propio bean. Es indispensable para que la anotacion
     * Async surta efecto: Spring la aplica por medio de un proxy, y una llamada
     * interna con this no pasa por ese proxy. La anotacion Lazy rompe el ciclo
     * de dependencia durante el arranque del contexto.
     */
    @Autowired
    @Lazy
    private FacturaService autoReferencia;

    /**
     * Genera una factura de forma sincrona (endpoint POST /v1/factura).
     * Comportamiento original sin cambios.
     */
    public Factura generarFactura(final Orden orden) throws InterruptedException {
        Factura factura = new Factura();

        String idFactura = generarIdFactura();
        facturas.put(idFactura, Optional.empty());

        Thread.sleep(5000);

        BigDecimal subtotal = calcularSubtotal(orden.getArticulos());
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(0.16)).setScale(2, RoundingMode.UP);
        BigDecimal total = subtotal.add(iva);

        factura.setId(idFactura);
        factura.setArticulos(orden.getArticulos());
        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setTotal(total);

        facturas.put(factura.getId(), Optional.of(factura));

        return factura;
    }

    /**
     * Inicia la generacion asincrona de una factura (endpoint POST /v2/factura).
     *
     * Registra el identificador como pendiente y delega el calculo a un hilo del
     * pool. Devuelve el identificador de inmediato, sin esperar a que la factura
     * quede terminada.
     *
     * @param orden orden de la que se derivara la factura
     * @return identificador con el que se podra consultar la factura
     */
    public String iniciarFacturaAsincrona(final Orden orden) throws InterruptedException {
        String idFactura = generarIdFactura();

        facturas.put(idFactura, Optional.empty());
        LOG.info("Factura {} registrada como pendiente; su calculo se delega al pool asincrono", idFactura);

        autoReferencia.crearFacturaAsincrona(idFactura, orden);

        return idFactura;
    }

    /**
     * Consulta el estado de una factura generada de forma asincrona
     * (endpoint GET /v2/factura/{idFactura}).
     *
     * @param idFactura identificador entregado por iniciarFacturaAsincrona
     * @return null si el identificador no existe, un Optional vacio si la factura
     *         sigue generandose, o un Optional con la factura ya terminada
     */
    public Optional<Factura> obtenerFacturaAsincrona(final String idFactura) {
        if (idFactura == null) {
            return null;
        }

        return facturas.get(idFactura);
    }

    /**
     * Realiza el calculo de la factura en un hilo distinto al de la peticion HTTP.
     *
     * Reproduce la misma logica de negocio de generarFactura, incluida la demora
     * de cinco segundos que simula el proceso pesado de facturacion.
     *
     * @param idFactura identificador reservado previamente
     * @param orden     orden de la que se derivara la factura
     */
    @Async
    public void crearFacturaAsincrona(final String idFactura, final Orden orden) throws InterruptedException {
        LOG.info("Inicia el calculo asincrono de la factura {} en el hilo {}",
                idFactura, Thread.currentThread().getName());

        facturas.putIfAbsent(idFactura, Optional.empty());

        Thread.sleep(5000);

        BigDecimal subtotal = calcularSubtotal(orden.getArticulos());
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(0.16)).setScale(2, RoundingMode.UP);
        BigDecimal total = subtotal.add(iva);

        Factura factura = new Factura();
        factura.setId(idFactura);
        factura.setArticulos(orden.getArticulos());
        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setTotal(total);

        facturas.put(idFactura, Optional.of(factura));

        LOG.info("Termina el calculo asincrono de la factura {} con un total de {}", idFactura, total);
    }

    private BigDecimal calcularSubtotal(List<Articulo> articulos) {
        BigDecimal subtotal = BigDecimal.ZERO;

        if (articulos != null) {
            for (final Articulo articulo : articulos) {
                BigDecimal precioUnitario = articulo.getPrecioUnitario();
                BigDecimal cantidad = BigDecimal.valueOf(articulo.getCantidad());
                BigDecimal totalArticulo = precioUnitario.multiply(cantidad);
                subtotal = subtotal.add(totalArticulo);
            }
        }

        return subtotal;
    }

    private String generarIdFactura() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public Map<String, Optional<Factura>> getFacturas() {
        return facturas;
    }

    public void setFacturas(Map<String, Optional<Factura>> facturas) {
        this.facturas = facturas;
    }
}
