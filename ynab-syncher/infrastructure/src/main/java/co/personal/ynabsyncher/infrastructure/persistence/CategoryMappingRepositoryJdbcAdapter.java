package co.personal.ynabsyncher.infrastructure.persistence;

import co.personal.ynabsyncher.model.Category;
import co.personal.ynabsyncher.model.CategoryMapping;
import co.personal.ynabsyncher.model.CategoryMappingId;
import co.personal.ynabsyncher.model.CategoryId;
import co.personal.ynabsyncher.model.CategoryType;
import co.personal.ynabsyncher.model.TransactionPattern;
import co.personal.ynabsyncher.spi.repository.CategoryMappingRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JDBC implementation of {@link CategoryMappingRepository} backed by PostgreSQL tables.
 * Persists mapping metadata in {@code category_mappings} and associated text patterns
 * in {@code category_mapping_patterns}.
 */
@Repository
public class CategoryMappingRepositoryJdbcAdapter implements CategoryMappingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CategoryMappingRepositoryJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "NamedParameterJdbcTemplate cannot be null");
    }

    @Override
    @Transactional
    public CategoryMapping save(CategoryMapping mapping) {
        Objects.requireNonNull(mapping, "Category mapping cannot be null");

        upsertMappingRow(mapping);
        replacePatterns(mapping);

        return mapping;
    }

    @Override
    @Transactional
    public List<CategoryMapping> saveAll(List<CategoryMapping> mappings) {
        Objects.requireNonNull(mappings, "Mappings collection cannot be null");
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("Mappings collection cannot be empty");
        }

        for (CategoryMapping mapping : mappings) {
            save(mapping);
        }

        return mappings;
    }

    @Override
    public List<CategoryMapping> findMappingsForPattern(TransactionPattern pattern) {
        Objects.requireNonNull(pattern, "Transaction pattern cannot be null");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("patterns", new ArrayList<>(pattern.textPatterns()));

        String sql = """
            SELECT m.id,
                   m.category_id,
                   m.category_name,
                   m.category_type,
                   m.confidence,
                   m.occurrence_count,
                   p.pattern
            FROM category_mappings m
            LEFT JOIN category_mapping_patterns p ON p.mapping_id = m.id
            WHERE m.id IN (
                SELECT DISTINCT m2.id
                FROM category_mappings m2
                JOIN category_mapping_patterns p2 ON p2.mapping_id = m2.id
                WHERE p2.pattern IN (:patterns)
            )
            ORDER BY m.confidence DESC, m.occurrence_count DESC
            """;

        return jdbcTemplate.query(sql, params, new CategoryMappingExtractor());
    }

    @Override
    public Optional<CategoryMapping> findBestMappingForPattern(TransactionPattern pattern) {
        return findMappingsForPattern(pattern).stream().findFirst();
    }

    @Override
    public List<CategoryMapping> findMappingsForCategory(Category category) {
        Objects.requireNonNull(category, "Category cannot be null");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("categoryId", category.id().value());

        String sql = """
            SELECT m.id,
                   m.category_id,
                   m.category_name,
                   m.category_type,
                   m.confidence,
                   m.occurrence_count,
                   p.pattern
            FROM category_mappings m
            LEFT JOIN category_mapping_patterns p ON p.mapping_id = m.id
            WHERE m.category_id = :categoryId
            ORDER BY m.confidence DESC, m.occurrence_count DESC
            """;

        return jdbcTemplate.query(sql, params, new CategoryMappingExtractor());
    }

    @Override
    public List<CategoryMapping> findMappingsContainingAnyPattern(List<String> textPatterns) {
        Objects.requireNonNull(textPatterns, "Text patterns cannot be null");
        if (textPatterns.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("patterns", new ArrayList<>(textPatterns));

        String sql = """
            SELECT m.id,
                   m.category_id,
                   m.category_name,
                   m.category_type,
                   m.confidence,
                   m.occurrence_count,
                   p.pattern
            FROM category_mappings m
            LEFT JOIN category_mapping_patterns p ON p.mapping_id = m.id
            WHERE m.id IN (
                SELECT DISTINCT m2.id
                FROM category_mappings m2
                JOIN category_mapping_patterns p2 ON p2.mapping_id = m2.id
                WHERE p2.pattern IN (:patterns)
            )
            ORDER BY m.confidence DESC, m.occurrence_count DESC
            """;

        return jdbcTemplate.query(sql, params, new CategoryMappingExtractor());
    }

    private void upsertMappingRow(CategoryMapping mapping) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", UUID.fromString(mapping.id().value()))
                .addValue("categoryId", mapping.category().id().value())
                .addValue("categoryName", mapping.category().name())
                .addValue("categoryType", mapping.category().type().name())
                .addValue("confidence", mapping.confidence())
                .addValue("occurrenceCount", mapping.occurrenceCount());

        String sql = """
            INSERT INTO category_mappings
                (id, category_id, category_name, category_type, confidence, occurrence_count)
            VALUES
                (:id, :categoryId, :categoryName, :categoryType, :confidence, :occurrenceCount)
            ON CONFLICT (id) DO UPDATE
                SET category_id = EXCLUDED.category_id,
                    category_name = EXCLUDED.category_name,
                    category_type = EXCLUDED.category_type,
                    confidence = EXCLUDED.confidence,
                    occurrence_count = EXCLUDED.occurrence_count,
                    updated_at = NOW()
            """;

        jdbcTemplate.update(sql, params);
    }

    private void replacePatterns(CategoryMapping mapping) {
        MapSqlParameterSource deleteParams = new MapSqlParameterSource()
                .addValue("mappingId", UUID.fromString(mapping.id().value()));

        jdbcTemplate.update(
                "DELETE FROM category_mapping_patterns WHERE mapping_id = :mappingId",
                deleteParams
        );

        if (mapping.textPatterns().isEmpty()) {
            return;
        }

        List<MapSqlParameterSource> batchParams = mapping.textPatterns().stream()
                .map(pattern -> new MapSqlParameterSource()
                        .addValue("mappingId", UUID.fromString(mapping.id().value()))
                        .addValue("pattern", pattern))
                .toList();

        String insertSql = """
            INSERT INTO category_mapping_patterns (mapping_id, pattern)
            VALUES (:mappingId, :pattern)
            ON CONFLICT (mapping_id, pattern) DO NOTHING
            """;

        jdbcTemplate.batchUpdate(insertSql, batchParams.toArray(MapSqlParameterSource[]::new));
    }

    private static class CategoryMappingExtractor implements ResultSetExtractor<List<CategoryMapping>> {

        @Override
        public List<CategoryMapping> extractData(@NonNull ResultSet rs) throws SQLException, DataAccessException {
            Map<UUID, CategoryMappingBuilder> builders = new LinkedHashMap<>();

            while (rs.next()) {
                UUID id = rs.getObject("id", UUID.class);
                CategoryMappingBuilder builder = builders.get(id);
                if (builder == null) {
                    builder = new CategoryMappingBuilder()
                            .withId(id)
                            .withCategory(
                                    rs.getString("category_id"),
                                    rs.getString("category_name"),
                                    rs.getString("category_type")
                            )
                            .withConfidence(rs.getDouble("confidence"))
                            .withOccurrenceCount(rs.getInt("occurrence_count"));
                    builders.put(id, builder);
                }

                String pattern = rs.getString("pattern");
                if (pattern != null) {
                    builder.addPattern(pattern);
                }
            }

            return builders.values().stream()
                    .map(CategoryMappingBuilder::build)
                    .collect(Collectors.toList());
        }
    }

    private static class CategoryMappingBuilder {
        private UUID id;
        private Category category;
        private double confidence;
        private int occurrenceCount;
        private final Set<String> patterns = new HashSet<>();

        CategoryMappingBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        CategoryMappingBuilder withCategory(String categoryId, String categoryName, String categoryType) {
            CategoryType type = CategoryType.valueOf(categoryType);
            this.category = new Category(CategoryId.of(categoryId), categoryName, type);
            return this;
        }

        CategoryMappingBuilder withConfidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        CategoryMappingBuilder withOccurrenceCount(int occurrenceCount) {
            this.occurrenceCount = occurrenceCount;
            return this;
        }

        CategoryMappingBuilder addPattern(String pattern) {
            this.patterns.add(pattern);
            return this;
        }

        CategoryMapping build() {
            if (patterns.isEmpty()) {
                throw new IllegalStateException("Persisted category mapping must have at least one pattern");
            }
            return new CategoryMapping(
                    CategoryMappingId.of(id.toString()),
                    category,
                    Set.copyOf(patterns),
                    confidence,
                    occurrenceCount
            );
        }
    }
}
