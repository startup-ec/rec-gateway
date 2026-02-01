# API Gateway con Spring Boot 3.5.4

API Gateway desarrollado con Java 17 y Spring Boot 3.5.4 usando Spring Cloud Gateway.

## Características

- **Java 17** con Spring Boot 3.5.4
- **Spring Cloud Gateway** para enrutamiento
- **Lombok** para reducir boilerplate
- **Filtros personalizados** para logging y headers
- **Manejo global de errores**
- **Endpoints de monitoreo** (health, info, routes)
- **CORS configurado**
- **Docker support**

## Estructura del Proyecto

```
src/main/java/com/gateway/
├── ApiGatewayApplication.java      # Clase principal
├── config/
│   └── GatewayConfig.java          # Configuración de rutas
├── controller/
│   └── GatewayController.java      # Endpoints de información
├── entity/
│   ├── RouteInfo.java              # Entidad de rutas (Lombok)
│   └── ErrorResponse.java          # Respuesta de errores (Lombok)
├── exception/
│   └── GlobalErrorHandler.java     # Manejo global de errores
└── filter/
    └── CustomGlobalFilter.java     # Filtro global personalizado
```

## Configuración de Rutas

El gateway está configurado para enrutar hacia:

- `/api/users/**` → `http://localhost:8081` (user-service)
- `/api/products/**` → `http://localhost:8082` (product-service)
- `/api/orders/**` → `http://localhost:8083` (order-service)

## Endpoints del Gateway

- `GET /gateway/routes` - Lista todas las rutas configuradas
- `GET /gateway/info` - Información del gateway
- `GET /gateway/health` - Estado de salud
- `GET /health` - Health check (redirect a actuator)

## Ejecución

### Requisitos
- Java 17
- Maven 3.6+

### Ejecutar localmente
```bash
mvn spring-boot:run
```

### Ejecutar con Docker
```bash
# Construir imagen
docker build -t api-gateway .

# Ejecutar contenedor
docker run -p 8080:8080 api-gateway
```

## Configuración

La configuración principal está en `application.yml`:

- Puerto: 8080
- Timeouts configurados
- CORS habilitado
- Logging detallado para desarrollo

## Características Técnicas

### Filtros Globales
- Logging de requests/responses
- Headers personalizados (X-Timestamp, X-Gateway-Request)
- Manejo de errores centralizados

### Entidades con Lombok
- `@Data` para getters/setters automáticos
- `@Builder` para patrón builder
- `@NoArgsConstructor` y `@AllArgsConstructor`

### Manejo de Errores
- Respuestas JSON estructuradas
- Logging de errores
- Diferentes tipos de error (404, 504, 500)
- Trace IDs para seguimiento

## Monitoreo

Endpoints de actuator habilitados:
- `/actuator/health` - Estado de salud
- `/actuator/info` - Información de la aplicación
- `/actuator/gateway/routes` - Rutas del gateway

## Desarrollo

Para agregar nuevas rutas, modificar `GatewayConfig.java`:

```java
.route("new-service", r -> r.path("/api/new/**")
    .filters(f -> f.stripPrefix(1))
    .uri("http://localhost:8084"))
```

Para personalizar filtros, extender `CustomGlobalFilter.java` o crear nuevos filtros.

## Notas

- No se usan imports explícitos en las clases (como solicitaste)
- Todas las entidades usan Lombok para reducir código
- Configuración reactiva con WebFlux
- Preparado para microservicios