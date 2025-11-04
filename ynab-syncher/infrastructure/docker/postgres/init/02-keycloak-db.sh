#!/bin/bash
set -e

# Create separate database for Keycloak
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create Keycloak database
    CREATE DATABASE keycloak;
    
    -- Grant access to keycloak database
    GRANT ALL PRIVILEGES ON DATABASE keycloak TO ynabsyncher;
    
    -- Connect to keycloak database and set up initial permissions
    \c keycloak
    GRANT ALL ON SCHEMA public TO ynabsyncher;
    
    -- Log successful initialization
    \echo 'Keycloak database created successfully'
EOSQL

echo "PostgreSQL initialization completed - Keycloak database ready"