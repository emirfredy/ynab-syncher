-- PostgreSQL Initialization Script
-- Creates additional database for Keycloak if needed

-- Create Keycloak database (separate from application database)
CREATE DATABASE keycloak;

-- Grant permissions to ynabsyncher user for Keycloak database
GRANT ALL PRIVILEGES ON DATABASE keycloak TO ynabsyncher;

-- Application database 'ynabsyncher' is created automatically by POSTGRES_DB
-- This script runs after the main database is created