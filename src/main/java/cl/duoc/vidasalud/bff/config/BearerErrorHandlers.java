package cl.duoc.vidasalud.bff.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Respuestas JSON homogeneas cuando el token falta o es invalido (401) y
 * cuando el rol no alcanza para el endpoint (403).
 */
public final class BearerErrorHandlers {

    private BearerErrorHandlers() {
    }

    private static void write(HttpServletResponse response, HttpStatus status, String code,
                              String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status.value(), code, message, path);
        response.getWriter().write(body);
    }

    /** 401: el JWT no llego, expiro, tiene mala firma, issuer o audience. */
    public static class EntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            response.setHeader("WWW-Authenticate", "Bearer");
            write(response, HttpStatus.UNAUTHORIZED, "unauthorized",
                    "Token ausente, expirado o no valido para esta API",
                    request.getRequestURI());
        }
    }

    /** 403: el token es valido pero el rol no habilita el endpoint. */
    public static class AccessDenied implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException accessDeniedException) throws IOException {
            write(response, HttpStatus.FORBIDDEN, "forbidden",
                    "El rol del usuario no permite consumir este endpoint",
                    request.getRequestURI());
        }
    }
}
