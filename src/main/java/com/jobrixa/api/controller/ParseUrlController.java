package com.jobrixa.api.controller;

import com.jobrixa.api.service.ParseUrlService;
import com.jobrixa.api.service.ParseUrlService.ParsedJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * POST /api/v1/applications/parse-url
 * Body:  { "url": "https://..." }
 * Fetches the job listing page and returns parsed metadata.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ParseUrlController {

    private final ParseUrlService parseUrlService;

    @PostMapping("/parse-url")
    public ResponseEntity<Map<String, String>> parseUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ParsedJob parsed = parseUrlService.parseUrl(url);
        return ResponseEntity.ok(Map.of(
                "companyName", parsed.companyName(),
                "jobTitle",    parsed.jobTitle(),
                "location",    parsed.location(),
                "source",      parsed.source(),
                "jobUrl",      parsed.jobUrl()
        ));
    }
}
