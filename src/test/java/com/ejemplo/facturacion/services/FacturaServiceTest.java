package com.ejemplo.facturacion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ejemplo.facturacion.valueobjects.Articulo;
import com.ejemplo.facturacion.valueobjects.Direccion;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

/**
 * Pruebas unitarias de FacturaService.
 *
 * Se cubren los cuatro metodos publicos de negocio de la clase:
 *   1. generarFactura            (sincrono, version 1)
 *   2. iniciarFacturaAsincrona   (arranque asincrono, version 2)
 *   3. crearFacturaAsincrona     (calculo en segundo plano, version 2)
 *   4. obtenerFacturaAsincrona   (consulta de estado, version 2)
 *
 * Las pruebas se ejecutan sobre el contexto real de Spring porque el
 * comportamiento asincrono depende del proxy que Spring genera a partir de la
 * anotacion Async. Sin ese contexto el metodo se ejecutaria de forma sincrona y
 * la prueba no validaria lo que realmente se quiere validar.
 *
 * Orden de ejemplo utilizada en todas las pruebas (la misma del enunciado):
 *   PANTALON  2 x 4.55 =  9.10
 *   CAMISA    3 x 7.88 = 23.64
 *   subtotal           = 32.74
 *   iva (16%)          =  5.24
 *   total              = 37.98
 */
@SpringBootTest
@DisplayName("Pruebas unitarias del servicio de facturacion")
class FacturaServiceTest {

    private static final String SUBTOTAL_ESPERADO = "32.74";
    private static final String IVA_ESPERADO = "5.24";
    private static final String TOTAL_ESPERADO = "37.98";

    /** Tiempo maximo de espera por una factura asincrona. */
    private static final long ESPERA_MAXIMA_MS = 20000L;

    /** Intervalo entre consultas mientras se espera una factura asincrona. */
    private static final long INTERVALO_SONDEO_MS = 200L;

    /**
     * Margen para comprobar que una llamada regresa sin esperar los cinco
     * segundos que tarda el calculo de la factura.
     */
    private static final long MARGEN_RESPUESTA_INMEDIATA_MS = 2000L;

    @Autowired
    private FacturaService facturaService;

    @BeforeEach
    void limpiarAlmacenDeFacturas() {
        facturaService.getFacturas().clear();
    }

    // ------------------------------------------------------------------
    // 1. generarFactura
    // ------------------------------------------------------------------

    @Test
    @DisplayName("generarFactura calcula subtotal, IVA y total, y registra la factura")
    void generarFacturaCalculaImportesYRegistraLaFactura() throws InterruptedException {
        Orden orden = construirOrdenDeEjemplo();

        Factura factura = facturaService.generarFactura(orden);

        assertNotNull(factura, "La factura no debe ser nula");
        assertNotNull(factura.getId(), "La factura debe tener un identificador");
        assertImporte(SUBTOTAL_ESPERADO, factura.getSubtotal(), "subtotal");
        assertImporte(IVA_ESPERADO, factura.getIva(), "IVA");
        assertImporte(TOTAL_ESPERADO, factura.getTotal(), "total");
        assertEquals(2, factura.getArticulos().size(), "La factura debe conservar los dos articulos");

        Optional<Factura> almacenada = facturaService.getFacturas().get(factura.getId());
        assertNotNull(almacenada, "La factura debe quedar registrada en el almacen");
        assertTrue(almacenada.isPresent(), "La factura registrada debe estar terminada");
        assertEquals(factura.getId(), almacenada.get().getId(), "Debe registrarse la misma factura");
    }

    @Test
    @DisplayName("generarFactura con una orden sin articulos devuelve importes en cero")
    void generarFacturaSinArticulosDevuelveImportesEnCero() throws InterruptedException {
        Orden orden = construirOrdenDeEjemplo();
        orden.setArticulos(new ArrayList<>());

        Factura factura = facturaService.generarFactura(orden);

        assertImporte("0", factura.getSubtotal(), "subtotal");
        assertImporte("0", factura.getIva(), "IVA");
        assertImporte("0", factura.getTotal(), "total");
    }

    // ------------------------------------------------------------------
    // 2. iniciarFacturaAsincrona
    // ------------------------------------------------------------------

    @Test
    @DisplayName("iniciarFacturaAsincrona devuelve el identificador sin esperar el calculo")
    void iniciarFacturaAsincronaDevuelveElIdentificadorDeInmediato() throws InterruptedException {
        Orden orden = construirOrdenDeEjemplo();

        long inicio = System.currentTimeMillis();
        String idFactura = facturaService.iniciarFacturaAsincrona(orden);
        long transcurrido = System.currentTimeMillis() - inicio;

        assertNotNull(idFactura, "Debe devolverse un identificador de factura");
        assertTrue(transcurrido < MARGEN_RESPUESTA_INMEDIATA_MS,
                () -> "La llamada debio responder de inmediato y tardo " + transcurrido + " ms");

        Optional<Factura> estado = facturaService.getFacturas().get(idFactura);
        assertNotNull(estado, "El identificador debe quedar registrado antes de responder");
        assertTrue(estado.isEmpty(), "La factura debe quedar marcada como pendiente");
    }

    @Test
    @DisplayName("iniciarFacturaAsincrona termina generando una factura con los importes correctos")
    void iniciarFacturaAsincronaTerminaGenerandoLaFactura() throws InterruptedException {
        Orden orden = construirOrdenDeEjemplo();

        String idFactura = facturaService.iniciarFacturaAsincrona(orden);
        Factura factura = esperarFacturaTerminada(idFactura);

        assertEquals(idFactura, factura.getId(), "La factura debe conservar el identificador entregado");
        assertImporte(SUBTOTAL_ESPERADO, factura.getSubtotal(), "subtotal");
        assertImporte(IVA_ESPERADO, factura.getIva(), "IVA");
        assertImporte(TOTAL_ESPERADO, factura.getTotal(), "total");
    }

    // ------------------------------------------------------------------
    // 3. crearFacturaAsincrona
    // ------------------------------------------------------------------

    @Test
    @DisplayName("crearFacturaAsincrona se ejecuta en segundo plano y deja la factura lista")
    void crearFacturaAsincronaSeEjecutaEnSegundoPlano() throws InterruptedException {
        Orden orden = construirOrdenDeEjemplo();
        String idFactura = "factura-de-prueba-asincrona";

        // Se reserva el identificador igual que lo hace iniciarFacturaAsincrona.
        facturaService.getFacturas().put(idFactura, Optional.empty());

        long inicio = System.currentTimeMillis();
        facturaService.crearFacturaAsincrona(idFactura, orden);
        long transcurrido = System.currentTimeMillis() - inicio;

        assertTrue(transcurrido < MARGEN_RESPUESTA_INMEDIATA_MS,
                () -> "El metodo debio delegarse a otro hilo y tardo " + transcurrido + " ms");

        Optional<Factura> enProceso = facturaService.getFacturas().get(idFactura);
        assertNotNull(enProceso, "La factura debe seguir registrada mientras se calcula");
        assertTrue(enProceso.isEmpty(), "La factura aun no debe estar terminada");

        Factura factura = esperarFacturaTerminada(idFactura);
        assertEquals(idFactura, factura.getId(), "La factura debe usar el identificador recibido");
        assertImporte(TOTAL_ESPERADO, factura.getTotal(), "total");
    }

    // ------------------------------------------------------------------
    // 4. obtenerFacturaAsincrona
    // ------------------------------------------------------------------

    @Test
    @DisplayName("obtenerFacturaAsincrona devuelve nulo cuando el identificador no existe")
    void obtenerFacturaAsincronaDevuelveNuloSiNoExiste() {
        assertNull(facturaService.obtenerFacturaAsincrona("identificador-inexistente"),
                "Un identificador desconocido debe devolver nulo para que el API responda 404");
        assertNull(facturaService.obtenerFacturaAsincrona(null),
                "Un identificador nulo debe tratarse como inexistente");
    }

    @Test
    @DisplayName("obtenerFacturaAsincrona devuelve un Optional vacio mientras la factura se genera")
    void obtenerFacturaAsincronaDevuelveVacioMientrasSeGenera() throws InterruptedException {
        String idFactura = facturaService.iniciarFacturaAsincrona(construirOrdenDeEjemplo());

        Optional<Factura> resultado = facturaService.obtenerFacturaAsincrona(idFactura);

        assertNotNull(resultado, "El identificador es valido, no debe devolverse nulo");
        assertTrue(resultado.isEmpty(),
                "Mientras la factura se genera debe devolverse un Optional vacio para responder 204");
    }

    @Test
    @DisplayName("obtenerFacturaAsincrona devuelve la factura cuando ya fue generada")
    void obtenerFacturaAsincronaDevuelveLaFacturaTerminada() throws InterruptedException {
        String idFactura = facturaService.iniciarFacturaAsincrona(construirOrdenDeEjemplo());
        esperarFacturaTerminada(idFactura);

        Optional<Factura> resultado = facturaService.obtenerFacturaAsincrona(idFactura);

        assertNotNull(resultado, "El identificador es valido, no debe devolverse nulo");
        assertTrue(resultado.isPresent(), "La factura ya terminada debe venir en el Optional");
        assertImporte(TOTAL_ESPERADO, resultado.get().getTotal(), "total");
    }

    // ------------------------------------------------------------------
    // Utilidades de la prueba
    // ------------------------------------------------------------------

    /** Construye la orden de ejemplo descrita en el enunciado del reto. */
    private Orden construirOrdenDeEjemplo() {
        Direccion direccion = new Direccion(
                "MX", "NUEVO LEON", "MONTERREY", "MONTERREY",
                "Eugenio Garza Sada", "2501 Sur", "81 8358 2000", "64700");

        List<Articulo> articulos = new ArrayList<>();
        articulos.add(new Articulo("PANTALON", 2L, new BigDecimal("4.55")));
        articulos.add(new Articulo("CAMISA", 3L, new BigDecimal("7.88")));

        Orden orden = new Orden();
        orden.setUsuario("egarza");
        orden.setDireccionCliente(direccion);
        orden.setArticulos(articulos);

        return orden;
    }

    /**
     * Consulta el almacen hasta que la factura queda terminada o se agota el
     * tiempo maximo de espera.
     */
    private Factura esperarFacturaTerminada(String idFactura) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA_MS;

        while (System.currentTimeMillis() < limite) {
            Optional<Factura> factura = facturaService.obtenerFacturaAsincrona(idFactura);

            if (factura != null && factura.isPresent()) {
                return factura.get();
            }

            Thread.sleep(INTERVALO_SONDEO_MS);
        }

        throw new AssertionError(
                "La factura " + idFactura + " no se genero dentro de " + ESPERA_MAXIMA_MS + " ms");
    }

    /** Compara importes por valor, sin que la escala decimal afecte el resultado. */
    private void assertImporte(String esperado, BigDecimal real, String concepto) {
        assertNotNull(real, () -> "El " + concepto + " no debe ser nulo");
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                () -> "El " + concepto + " esperado era " + esperado + " y se obtuvo " + real);
    }
}
