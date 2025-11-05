package co.personal.ynabsyncher.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a domain service implementing business logic.
 * 
 * Domain services are framework-free implementations of use cases that contain
 * complex business logic that doesn't naturally fit within a single entity.
 * They should only depend on domain repositories and other domain services.
 * 
 * This annotation is purely for domain documentation and infrastructure discovery.
 * NO Spring framework dependencies to maintain hexagonal architecture compliance.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainService {
    
    /**
     * Optional logical component name for infrastructure layer registration.
     * @return the suggested component name, if any (or empty String otherwise)
     */
    String value() default "";
}
