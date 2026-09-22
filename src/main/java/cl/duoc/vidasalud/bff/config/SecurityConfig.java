package cl.duoc.vidasalud.bff.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de seguridad del BFF.
 *
 * Igual que el JWT Authorizer del API Gateway, aqui se valida:
 *  - la firma del token contra las claves publicas del IDaaS (JWKS),
 *  - el issuer,
 *  - la audiencia,
 *  - la vigencia (exp / nbf).
 * Ademas se traduce el claim "roles" de Azure AD a authorities ROLE_*.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final SecurityProperties properties;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    public SecurityConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Catalogo: lo lee tambien el paciente, porque necesita elegir
                // la prestacion al solicitar una atencion. Escribir, solo admin.
                .requestMatchers(HttpMethod.GET, "/api/catalog/**")
                    .hasAnyRole("ADMIN", "RECEPCIONISTA", "PACIENTE")
                .requestMatchers("/api/catalog/**").hasRole("ADMIN")

                // Atenciones: el paciente puede crear y consultar las suyas.
                .requestMatchers(HttpMethod.POST, "/api/appointments")
                    .hasAnyRole("ADMIN", "RECEPCIONISTA", "PACIENTE")
                .requestMatchers(HttpMethod.PUT, "/api/appointments/*/status")
                    .hasAnyRole("ADMIN", "RECEPCIONISTA")
                .requestMatchers("/api/appointments/**")
                    .hasAnyRole("ADMIN", "RECEPCIONISTA", "PACIENTE")

                // Reporteria y auditoria: solo lectura y roles restringidos.
                .requestMatchers(HttpMethod.GET, "/api/report/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")

                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new BearerErrorHandlers.EntryPoint())
                .accessDeniedHandler(new BearerErrorHandlers.AccessDenied()));

        return http.build();
    }

    /**
     * Decoder con validadores encadenados: los por defecto del issuer
     * (firma, exp, nbf, iss) mas la comprobacion de audiencia.
     */
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience =
                new AudienceValidator(properties.getAllowedAudiences());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }

    /**
     * Azure AD entrega los App Roles en el claim "roles" (y los scopes en "scp").
     * Spring Security espera authorities con prefijo ROLE_ para hasRole().
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(r ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())));
            }

            String scp = jwt.getClaimAsString("scp");
            if (scp != null) {
                for (String scope : scp.split(" ")) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
