CREATE TABLE IF NOT EXISTS category_mappings (
    id UUID PRIMARY KEY,
    category_id VARCHAR(120) NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    category_type VARCHAR(50) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    occurrence_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS category_mapping_patterns (
    mapping_id UUID NOT NULL REFERENCES category_mappings(id) ON DELETE CASCADE,
    pattern TEXT NOT NULL,
    PRIMARY KEY (mapping_id, pattern)
);

CREATE INDEX IF NOT EXISTS idx_category_mapping_pattern ON category_mapping_patterns(pattern);
CREATE INDEX IF NOT EXISTS idx_category_mappings_category ON category_mappings(category_id);
