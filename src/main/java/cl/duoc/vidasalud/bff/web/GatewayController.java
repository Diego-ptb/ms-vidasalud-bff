package cl.duoc.vidasalud.bff.web;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.vidasalud.bff.client.DownstreamClient;
import cl.duoc.vidasalud.bff.config.DownstreamProperties;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Enruta las llamadas ya autenticadas y autorizadas hacia el microservicio de
 * dominio correspondiente. Cadena completa:
 * JWT -> API Gateway -> BFF -> microservicio.
 */
@RestController
public class GatewayController {

    private final DownstreamClient client;
    private final DownstreamProperties downstream;

    public GatewayController(DownstreamClient client, DownstreamProperties downstream) {
        this.client = client;
        this.downstream = downstream;
    }

    @RequestMapping("/api/appointments/**")
    public ResponseEntity<String> appointments(HttpServletRequest request,
                                               @RequestBody(required = false) String body,
                                               Authentication auth) {
        return forward(downstream.getAppointments(), request, body, auth);
    }

    @RequestMapping("/api/catalog/**")
    public ResponseEntity<String> catalog(HttpServletRequest request,
                                          @RequestBody(required = false) String body,
                                          Authentication auth) {
        return forward(downstream.getCatalog(), request, body, auth);
    }

    @RequestMapping("/api/report/**")
    public ResponseEntity<String> report(HttpServletRequest request,
                                         @RequestBody(required = false) String body,
                                         Authentication auth) {
        return forward(downstream.getReport(), request, body, auth);
    }

    @RequestMapping("/api/audit/**")
    public ResponseEntity<String> audit(HttpServletRequest request,
                                        @RequestBody(required = false) String body,
                                        Authentication auth) {
        return forward(downstream.getAudit(), request, body, auth);
    }

    /**
     * Email del usuario segun el token ya validado. Los microservicios lo usan
     * para acotar los datos que devuelven (un paciente solo ve lo suyo), asi
     * que tiene que salir del token y nunca de un parametro del cliente.
     */
    private String emailDe(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String email = jwt.getClaimAsString("preferred_username");
            if (email == null) {
                email = jwt.getClaimAsString("email");
            }
            if (email == null) {
                email = jwt.getClaimAsString("upn");
            }
            return email == null ? "" : email;
        }
        return "";
    }

    private ResponseEntity<String> forward(String baseUrl, HttpServletRequest request,
                                           String body, Authentication auth) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String target = baseUrl + path + (query != null ? "?" + query : "");

        String roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.joining(","));

        ResponseEntity<String> response = client.exchange(
                HttpMethod.valueOf(request.getMethod()),
                URI.create(target).toString(),
                body,
                auth.getName(),
                emailDe(auth),
                roles);

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }
}
