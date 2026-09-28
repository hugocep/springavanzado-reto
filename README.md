# API de Facturación — segura, asíncrona y lista para desplegar

Reto de la subcompetencia de Spring avanzado. El servicio original de generación
de facturas se mejoró en cuatro frentes: desempeño mediante un esquema asíncrono,
pruebas unitarias con JUnit 5, autenticación y autorización con tokens JWT, y
empaquetado con scripts de arranque y paro.

**Autor:** Hugo Ernesto Cervantes Ponce
**Plataforma:** The Learning Gate — Tecnológico de Monterrey

---

## Requisitos

| Herramienta | Versión | Nota |
|---|---|---|
| JDK | 21 | Eclipse Temurin recomendado |
| Maven | — | se usa el wrapper incluido (`mvnw` / `mvnw.cmd`) |
| Postman | cualquiera | para reproducir las evidencias |

No se necesita base de datos: se usa H2 en memoria.

## Cómo ejecutarlo

```bash
# 1. Compilar, correr las pruebas y empaquetar
./mvnw clean package          # Windows:  mvnw.cmd clean package

# 2. Arrancar el servicio
./bin/iniciar.sh              # Windows:  bin\iniciar.bat

# 3. Verificar
curl http://localhost:8080/actuator/health

# 4. Detener el servicio
./bin/detener.sh              # Windows:  bin\detener.bat
```

La bitácora de consola queda en `logs/facturacion.log`.

## Endpoints

| Método | Ruta | Token | Descripción |
|---|---|---|---|
| GET | `/actuator/health` | no | Estado del servicio |
| POST | `/autenticar` | no | Devuelve un token JWT (`developer` / `12345`) |
| POST | `/v1/factura` | sí | Genera la factura de forma síncrona → `201` |
| POST | `/v2/factura` | sí | Acepta la orden y factura en segundo plano → `202` |
| GET | `/v2/factura/{idFactura}` | sí | `200` lista · `204` en proceso · `404` no existe |

### Obtener un token

```bash
curl -X POST http://localhost:8080/autenticar \
     -H "Content-Type: application/json" \
     -d '{"usuario":"developer","contraseña":"12345"}'
```

### Usarlo

```bash
curl -X POST http://localhost:8080/v2/factura \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <TOKEN>" \
     -d @orden.json -i
```

La respuesta trae `202 Accepted` con los encabezados `Location`, `Retry-After`
e `Id-Factura`.

## Estructura relevante

```
bin/                          scripts de arranque y paro (Windows y Unix)
postman/                      colección de Postman con las nueve pruebas
src/main/java/.../services/FacturaService.java          lógica síncrona y asíncrona
src/main/java/.../controllers/FacturacionV2Controller.java   endpoints v2
src/main/java/.../config/GestionUsuariosConfig.java     reglas de seguridad JWT
src/test/java/.../services/FacturaServiceTest.java      pruebas unitarias
```

## Pruebas

```bash
./mvnw test
```

Ocho pruebas cubren los cuatro métodos públicos de `FacturaService`. Las pruebas
del esquema asíncrono verifican tanto que la llamada regrese de inmediato como
que la factura termine con los importes correctos.

## Decisiones de diseño

- **`@Async` con auto-referencia `@Lazy`.** Spring aplica `@Async` mediante un
  proxy; una llamada interna con `this` lo evitaría y el método correría de forma
  síncrona. Por eso `iniciarFacturaAsincrona` invoca a `crearFacturaAsincrona` a
  través del bean auto-inyectado.
- **`ConcurrentHashMap` para el almacén de facturas.** El mapa lo escriben los
  hilos del pool mientras los hilos de las peticiones lo leen.
- **Tres estados en un solo mapa.** Llave ausente → `404`; llave con `Optional`
  vacío → `204`; llave con valor → `200`.
- **`202 Accepted` en el POST de v2.** Es el código previsto para peticiones
  aceptadas cuyo procesamiento todavía no concluye.
- **Sesiones sin estado y sin autenticación básica.** La única vía de acceso es
  el token emitido por `/autenticar`.
