package cl.duoc.vidasalud.bff.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.vidasalud.bff.config.SecurityProperties;

/**
 * Cliente HTTP hacia los microservicios internos.
 *
 * El JWT del usuario NO se reenvia: el BFF ya lo valido. Hacia adentro se
 * propaga la identidad en cabeceras (X-User, X-User-Email, X-Roles) y una clave compartida
 * (X-Internal-Key) para que ningun cliente externo pueda golpear los servicios
 * de dominio directamente.
 */
@Component
public class DownstreamClient {

    private final RestClient restClient;
    private final SecurityProperties securityProperties;

    public DownstreamClient(RestClient.Builder builder, SecurityProperties securityProperties) {
        this.restClient = builder.build();
        this.securityProperties = securityProperties;
    }

    public ResponseEntity<String> exchange(HttpMethod method, String url, String body,
                                           String user, String userEmail, String roles) {
        RestClient.RequestBodySpec spec = restClient
                .method(method)
                .uri(url)
                .header("X-Internal-Key", securityProperties.getInternalKey())
                .header("X-User", user)
                .header("X-User-Email", userEmail)
                .header("X-Roles", roles)
                .accept(MediaType.APPLICATION_JSON);

        if (body != null && !body.isBlank()) {
            spec = spec.contentType(MediaType.APPLICATION_JSON);
            return spec.body(body).retrieve().toEntity(String.class);
        }
        return spec.retrieve().toEntity(String.class);
    }
}
