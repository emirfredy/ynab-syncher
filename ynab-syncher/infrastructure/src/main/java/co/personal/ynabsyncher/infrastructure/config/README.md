# SecurityConfig Documentation

## Overview

The `SecurityConfig` class implements conditional security configuration for YNAB Syncher, supporting both development and production authentication modes while maintaining hexagonal architecture principles.

## Architecture

Authentication is purely an infrastructure concern and doesn't affect the domain layer.

## Configuration Modes

### 1. Development Mode (Default Profile)

- **Trigger**: `app.auth.external-validation.enabled=false` (default)
- **Behavior**: No authentication required - permits all requests
- **Use Case**: Rapid development with H2 database
- **Security**: CSRF disabled, CORS disabled, stateless sessions

### 2. OAuth2 Mode (Docker Profile - Ready but Disabled)

- **Trigger**: `app.auth.external-validation.enabled=true`
- **Behavior**: JWT validation from external OAuth2 provider (Keycloak)
- **Use Case**: Production-like environment with PostgreSQL + Keycloak
- **Security**: OAuth2 Resource Server with JWT, stateless sessions

## Public Endpoints (Always Accessible)

- `/actuator/health` - Health checks
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/h2-console/**` - H2 database console (development)

## Configuration Properties

- `app.auth.external-validation.enabled`: Boolean flag controlling authentication mode
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`: Keycloak issuer URI for JWT validation

## Phase Implementation Status

- **Phase 4**: ✅ Basic JWT validation infrastructure ready but disabled
- **Phase 5**: Keycloak realm setup with users and roles (future)
- **Phase 6**: Enable authentication by setting `app.auth.external-validation.enabled=true` in docker profile

## Testing Strategy

Both modes produce identical behavior (no authentication) until Phase 6 when OAuth2 is enabled in docker profile only.
