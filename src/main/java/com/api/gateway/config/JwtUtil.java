package com.api.gateway.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@org.springframework.stereotype.Component
@lombok.extern.slf4j.Slf4j
public class JwtUtil {
    public  String secret = "mySecretKey123456789012345678901234567890";

    public String getUsernameFromToken(String token) {
        try {

            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            // Si tienes un claim específico para userId, úsalo.
            // Si no, puedes usar el subject (username)
            return decodedJWT.getSubject(); // o decodedJWT.getClaim("userId").asString();
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    public List<String> getRolesFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("cognito:roles").asList(String.class);
        } catch (Exception e) {
            log.error("Error extracting roles from token: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<String> getPermissionsFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("cognito:permissions").asList(String.class);
        } catch (Exception e) {
            log.error("Error extracting permissions from token: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Boolean validateToken(String token) {
        try {
            log.debug("Validating JWT token");

            JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);

            log.debug("JWT token validation successful");
            return true;

        } catch (Exception e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getExpiresAt().before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    // Métodos adicionales para extraer claims específicos
    public String getEmailFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("email").asString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getFirstNameFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("given_name").asString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getLastNameFromToken(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(secret))
                    .build()
                    .verify(token);
            return decodedJWT.getClaim("family_name").asString();
        } catch (Exception e) {
            return null;
        }
    }
}