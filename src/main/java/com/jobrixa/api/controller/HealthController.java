package com.jobrixa.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight health/ping endpoint.
 * Lives under /api/v1/auth/** so it's publicly accessible (no JWT needed).
 * Used by the frontend to keep the Render free-tier backend warm.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class HealthController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
