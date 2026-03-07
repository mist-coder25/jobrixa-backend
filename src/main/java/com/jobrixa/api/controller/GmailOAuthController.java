package com.jobrixa.api.controller;

import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.UserRepository;
import com.jobrixa.api.service.GmailScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gmail OAuth endpoints:
 *  GET  /api/v1/gmail/auth-url   → returns Google consent URL
 *  GET  /api/v1/gmail/callback   → exchanges code for refresh token
 *  POST /api/v1/gmail/scan       → triggers email scan for current user
 *  GET  /api/v1/gmail/status     → returns connection status + last scan time
 *  POST /api/v1/gmail/disconnect → clears stored tokens
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gmail")
@RequiredArgsConstructor
public class GmailOAuthController {

    @Value("${google.client.id:}")
    private String clientId;

    @Value("${google.redirect.uri:}")
    private String redirectUri;

    private final GmailScanService gmailScanService;
    private final UserRepository userRepository;

    /** Returns the Google OAuth consent URL the frontend should redirect to. */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (clientId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "GOOGLE_CLIENT_ID not configured on server"));
        }

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/gmail.readonly"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + java.net.URLEncoder.encode(userDetails.getUsername(), java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * Google redirects here after user approves.
     * Exchanges the code for tokens and saves the refresh token.
     * Redirects user back to /settings on completion.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        try {
            if (state == null || state.isBlank()) {
                log.warn("Gmail callback received without state param");
                return ResponseEntity.status(302)
                        .header("Location", "https://jobrixa-frontend.vercel.app/settings?gmail=error")
                        .build();
            }
            gmailScanService.exchangeCodeAndSave(code, state);
            return ResponseEntity.status(302)
                    .header("Location", "https://jobrixa-frontend.vercel.app/settings?gmail=connected")
                    .build();
        } catch (Exception e) {
            log.error("Gmail callback error: {}", e.getMessage());
            return ResponseEntity.status(302)
                    .header("Location", "https://jobrixa-frontend.vercel.app/settings?gmail=error")
                    .build();
        }
    }

    /** Triggers a manual email scan for the authenticated user. */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            int detected = gmailScanService.scanEmails(userDetails.getUsername());
            return ResponseEntity.ok(Map.of("detected", detected, "status", "ok"));
        } catch (Exception e) {
            log.error("Gmail scan error for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Returns Gmail connection status and last scan time. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        boolean connected = user.getGmailRefreshToken() != null && !user.getGmailRefreshToken().isBlank();
        String lastScanned = user.getGmailLastScanned() != null
                ? user.getGmailLastScanned().toString()
                : "Never";
        return ResponseEntity.ok(Map.of("connected", connected, "lastScanned", lastScanned));
    }

    /** Disconnects Gmail by clearing the stored refresh token. */
    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        user.setGmailRefreshToken(null);
        user.setGmailConnected(false);
        user.setGmailLastScanned(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", "disconnected"));
    }
}
