package co.personal.ynabsyncher.infrastructure.persistence.jpa;

import co.personal.ynabsyncher.infrastructure.persistence.entity.CategoryMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for CategoryMappingEntity.
 * Provides custom query methods for category mapping operations.
 */
@Repository
public interface CategoryMappingJpaRepository extends JpaRepository<CategoryMappingEntity, UUID> {

    /**
     * Find mappings that contain any of the specified text patterns.
     * Ordered by confidence and occurrence count for best matching.
     */
    @Query("""
        SELECT DISTINCT m FROM CategoryMappingEntity m 
        JOIN FETCH m.patterns p 
        WHERE p.pattern IN :patterns 
        ORDER BY m.confidence DESC, m.occurrenceCount DESC
        """)
    List<CategoryMappingEntity> findByPatternsContainingAnyPattern(@Param("patterns") List<String> patterns);

    /**
     * Find all mappings for a specific category.
     * Ordered by confidence and occurrence count.
     */
    @Query("""
        SELECT m FROM CategoryMappingEntity m 
        JOIN FETCH m.patterns 
        WHERE m.categoryId = :categoryId 
        ORDER BY m.confidence DESC, m.occurrenceCount DESC
        """)
    List<CategoryMappingEntity> findByCategoryIdOrderByConfidenceDesc(@Param("categoryId") String categoryId);

    /**
     * Find the best mapping (highest confidence) for patterns.
     * Returns the single best match based on confidence and occurrence count.
     */
    @Query("""
        SELECT m FROM CategoryMappingEntity m 
        JOIN FETCH m.patterns p 
        WHERE p.pattern IN :patterns 
        ORDER BY m.confidence DESC, m.occurrenceCount DESC 
        LIMIT 1
        """)
    CategoryMappingEntity findBestMappingForPatterns(@Param("patterns") List<String> patterns);

    /**
     * Find mappings with confidence greater than threshold.
     * Useful for high-confidence automatic categorization.
     */
    @Query("""
        SELECT m FROM CategoryMappingEntity m 
        JOIN FETCH m.patterns 
        WHERE m.confidence >= :minConfidence 
        ORDER BY m.confidence DESC, m.occurrenceCount DESC
        """)
    List<CategoryMappingEntity> findByConfidenceGreaterThanEqual(@Param("minConfidence") Double minConfidence);

    /**
     * Find mappings with occurrence count greater than threshold.
     * Useful for finding well-established patterns.
     */
    @Query("""
        SELECT m FROM CategoryMappingEntity m 
        JOIN FETCH m.patterns 
        WHERE m.occurrenceCount >= :minOccurrences 
        ORDER BY m.confidence DESC, m.occurrenceCount DESC
        """)
    List<CategoryMappingEntity> findByOccurrenceCountGreaterThanEqual(@Param("minOccurrences") Integer minOccurrences);
}