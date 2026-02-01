package com.api.gateway.filter;

import com.api.gateway.config.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@lombok.RequiredArgsConstructor
public class GatewayJwtAuthenticationFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayJwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        logger.debug("Processing JWT authentication for path: {}", request.getPath());

        // Obtener el token del header Authorization
        String token = extractToken(request);

        if (token == null) {
            logger.warn("No JWT token found in Authorization header for path: {}", request.getPath());
            return unauthorizedResponse(exchange);
        }

        try {
            // Validar el token
            if (!jwtUtil.validateToken(token)) {
                logger.warn("Invalid JWT token for path: {}", request.getPath());
                return unauthorizedResponse(exchange);
            }

            // Extraer información del usuario del token
            String username = jwtUtil.getUsernameFromToken(token);
            String userId = jwtUtil.getUserIdFromToken(token); // Ahora usando el método correcto
            List<String> rolesList = jwtUtil.getRolesFromToken(token);
            List<String> permissionsList = jwtUtil.getPermissionsFromToken(token);

            String roles = String.join(",", rolesList);
            String permissions = String.join(",", permissionsList);

            logger.debug("JWT validated successfully for user: {} with roles: {} and permissions: {}",
                    username, roles, permissions);

            // Agregar headers con la información del usuario para los microservicios
            ServerHttpRequest.Builder requestBuilder = request.mutate()
                    .headers(headers -> headers.remove("Authorization")) // Remover JWT original
                    .header("X-User-Name", username)
                    .header("X-User-Roles", roles)
                    .header("X-User-Permissions", permissions)
                    .header("X-Authenticated", "true");

            // Solo agregar X-User-Id si existe
            if (userId != null && !userId.isEmpty()) {
                requestBuilder.header("X-User-Id", userId);
            }

            // Opcionalmente, agregar más información del usuario
            String email = jwtUtil.getEmailFromToken(token);
            if (email != null && !email.isEmpty()) {
                requestBuilder.header("X-User-Email", email);
            }

            String firstName = jwtUtil.getFirstNameFromToken(token);
            if (firstName != null && !firstName.isEmpty()) {
                requestBuilder.header("X-User-First-Name", firstName);
            }

            String lastName = jwtUtil.getLastNameFromToken(token);
            if (lastName != null && !lastName.isEmpty()) {
                requestBuilder.header("X-User-Last-Name", lastName);
            }

            ServerHttpRequest modifiedRequest = requestBuilder.build();

            logger.debug("Added authentication headers to request for downstream services");

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage(), e);
            return unauthorizedResponse(exchange);
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}