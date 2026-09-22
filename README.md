# ms-vidasalud-bff

BFF (Backend for Frontend) del caso **VidaSalud** · DSY1107 Desarrollo Cloud Native I.

Es el único componente del backend expuesto al exterior. Valida el JWT emitido por
Microsoft Entra ID y enruta hacia los microservicios de dominio.

```
Navegador (React + MSAL)
      │  Authorization: Bearer <access_token>
      ▼
AWS API Gateway (JWT Authorizer)
      ▼
ms-vidasalud-bff          ← este repositorio
      │  X-Internal-Key + X-User + X-User-Email + X-Roles
      ▼
ms-vidasalud-appointments · ms-vidasalud-catalog
```

## Qué valida

| Comprobación | Dónde |
|---|---|
| Firma del token contra el JWKS del tenant | `SecurityConfig.jwtDecoder` |
| Issuer (`iss`) | `JwtValidators.createDefaultWithIssuer` |
| Audiencia (`aud`) | `AudienceValidator` |
| Vigencia (`exp` / `nbf`) | validadores por defecto del issuer |
| Rol por endpoint | `SecurityConfig.filterChain` |

El claim `roles` de Entra ID se traduce a authorities `ROLE_*`, y los `scp` a `SCOPE_*`.
Las respuestas de error son JSON propio: **401** si el token falta o no es válido, **403**
si el rol no alcanza (`BearerErrorHandlers`).

El JWT del usuario **no** se reenvía hacia adentro. La identidad se propaga en cabeceras y
los microservicios exigen una clave compartida (`X-Internal-Key`), de modo que nadie pueda
saltarse esta capa.

## Autorización por rol

| Endpoint | Roles |
|---|---|
| `GET /api/catalog/**` | ADMIN, RECEPCIONISTA, PACIENTE |
| `POST·PUT·DELETE /api/catalog/**` | ADMIN |
| `POST /api/appointments` | ADMIN, RECEPCIONISTA, PACIENTE |
| `PUT /api/appointments/*/status` | ADMIN, RECEPCIONISTA |
| `GET /api/report/**` | ADMIN |
| `GET /api/audit/**` | ADMIN, AUDITOR |

## Ejecutar en local

Requiere **JDK 17+** (Spring Boot 3.3 no funciona con Java 8).

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$env:AZURE_TENANT_ID = "<tenant id>"
$env:AZURE_API_CLIENT_ID = "<client id del App Registration de la API>"
$env:FRONTEND_ORIGIN = "http://localhost:4200"
$env:INTERNAL_KEY = "<clave compartida con los microservicios>"

mvn spring-boot:run
```

Queda en `http://localhost:8080`. Sin `AZURE_TENANT_ID` no arranca: necesita descargar las
claves públicas del tenant para verificar firmas.

## Variables de entorno

| Variable | Descripción |
|---|---|
| `AZURE_TENANT_ID` | Tenant de Microsoft Entra ID |
| `AZURE_API_CLIENT_ID` | Client ID de la API; define la audiencia aceptada |
| `AZURE_ISSUER_URI` | Opcional. Sobrescribe el issuer si no es Entra ID estándar |
| `FRONTEND_ORIGIN` | Origen permitido para CORS |
| `INTERNAL_KEY` | Clave compartida con los microservicios de dominio |
| `MS_APPOINTMENTS_URL` · `MS_CATALOG_URL` · `MS_REPORT_URL` · `MS_AUDIT_URL` | URLs internas |

## Comprobar que valida

```bash
curl http://localhost:8080/api/me                              # 401
curl http://localhost:8080/api/me -H "Authorization: Bearer x" # 401
```

Con un token válido, `GET /api/me` devuelve los claims que el BFF leyó **después** de
validarlo: subject, roles, issuer, audiencia y expiración.

## Stack

Java 17 · Spring Boot 3.3.5 · Spring Security OAuth2 Resource Server · Maven · Docker
