-- Phase 3: Account-Level Authorization - Account Ownership Table
-- Creates the table for managing user-to-account ownership and permissions

CREATE TABLE account_ownership (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL,
    account_id      VARCHAR(255) NOT NULL,
    permission      VARCHAR(20) NOT NULL CHECK (permission IN ('OWNER', 'READ_WRITE', 'READ_ONLY')),
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(255) NOT NULL,
    updated_at      TIMESTAMP(6),
    updated_by      VARCHAR(255),
    
    -- Ensure unique user-account combinations
    CONSTRAINT uk_account_ownership_user_account UNIQUE (user_id, account_id)
);

-- Performance indexes for efficient access control queries
CREATE INDEX idx_account_ownership_user_id ON account_ownership (user_id);
CREATE INDEX idx_account_ownership_account_id ON account_ownership (account_id);
CREATE INDEX idx_account_ownership_permission ON account_ownership (permission);

-- Composite index for the most common query pattern (user accessing specific account)
CREATE INDEX idx_account_ownership_user_account ON account_ownership (user_id, account_id);

-- Comments for documentation
COMMENT ON TABLE account_ownership IS 'Maps users to financial accounts they can access with specific permission levels';
COMMENT ON COLUMN account_ownership.user_id IS 'The authenticated user identifier from JWT token';
COMMENT ON COLUMN account_ownership.account_id IS 'The financial account identifier (bank account, YNAB account, etc.)';
COMMENT ON COLUMN account_ownership.permission IS 'The level of access granted: OWNER (full), READ_WRITE (modify), READ_ONLY (view)';
COMMENT ON COLUMN account_ownership.created_at IS 'When this ownership relationship was established';
COMMENT ON COLUMN account_ownership.created_by IS 'Who created this ownership relationship (user or system)';
COMMENT ON COLUMN account_ownership.updated_at IS 'When permissions were last modified';
COMMENT ON COLUMN account_ownership.updated_by IS 'Who last modified the permissions';

-- Sample data for testing Phase 3 functionality
-- Note: These would typically be created through account registration flows
INSERT INTO account_ownership (id, user_id, account_id, permission, created_by) VALUES
    ('test-ownership-1', 'user123', 'account-456', 'OWNER', 'system'),
    ('test-ownership-2', 'user123', 'account-789', 'READ_WRITE', 'system'),
    ('test-ownership-3', 'admin123', 'account-456', 'OWNER', 'system'),
    ('test-ownership-4', 'user456', 'account-999', 'OWNER', 'system'),
    ('test-ownership-5', 'readonly-user', 'account-456', 'READ_ONLY', 'admin123');