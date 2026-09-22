package cl.duoc.vidasalud.bff.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros del IDaaS (Azure AD) que el BFF debe verificar en cada token.
 */
@ConfigurationProperties(prefix = "vidasalud.security")
public class SecurityProperties {

    /** Audiencias aceptadas, p.ej. api://<API_CLIENT_ID> y el propio clientId. */
    private List<String> allowedAudiences = List.of();

    /** Orígenes permitidos para CORS (URL del frontend). */
    private List<String> allowedOrigins = List.of("http://localhost:4200");

    /** Clave compartida que el BFF envía a los microservicios internos. */
    private String internalKey = "vidasalud-internal";

    public List<String> getAllowedAudiences() {
        return allowedAudiences;
    }

    public void setAllowedAudiences(List<String> allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getInternalKey() {
        return internalKey;
    }

    public void setInternalKey(String internalKey) {
        this.internalKey = internalKey;
    }
}
