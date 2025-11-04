# Application-Docker Profile Hostname Configuration

#

# When running the application in docker profile outside containers,

# use localhost for database connections.

#

# When running inside containers (future), override with environment variables:

# SPRING_DATASOURCE_URL=jdbc:postgresql://ynab-postgres:5432/ynabsyncher

# KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/ynab-syncher

## Current Configuration (Phase 3)

- **Outside Containers**: localhost connections (development testing)
- **Inside Containers**: Override with service hostnames via environment variables

## Container Environment Variables (Future Phases)

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://ynab-postgres:5432/ynabsyncher
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/ynab-syncher
KEYCLOAK_SERVER_URL=http://keycloak:8080
```

This allows the same docker profile to work both for:

1. **Local Development**: Application outside containers, connecting to containerized services
2. **Full Containerization**: Application inside containers, using service discovery
