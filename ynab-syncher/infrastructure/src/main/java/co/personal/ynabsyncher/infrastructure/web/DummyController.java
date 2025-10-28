package co.personal.ynabsyncher.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dummy controller for initial setup - to be replaced with actual controllers.
 */
@RestController
public class DummyController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}