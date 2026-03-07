package com.jobrixa.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobrixa.api.entity.JobApplication;
import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.JobApplicationRepository;
import com.jobrixa.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles Gmail OAuth token exchange and email scanning.
 * Uses plain Java 11 HttpClient + Jackson to avoid heavy Google SDK dependencies.
 * Reads only subject + snippet — never full email body.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailScanService {

    @Value("${google.client.id:}")
    private String clientId;

    @Value("${google.client.secret:}")
    private String clientSecret;

    @Value("${google.redirect.uri:}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Email pattern rules ────────────────────────────────────────────────────

    private static final Map<String, String> STATUS_PATTERNS = Map.of(
        "APPLIED",      "thank you for applying|application received|application submitted|we received your application|application confirmation",
        "OA",           "online assessment|coding assessment|take a test|complete the following assessment|hackerrank|codility|codesignal|assessment link|aptitude test",
        "INTERVIEW",    "interview scheduled|interview invitation|we would like to invite you|schedule your interview|interview slot|interview request",
        "REJECTED",     "we will not be moving forward|not selected|unfortunately|we have decided|other candidates|not moving forward|position has been filled",
        "OFFER",        "offer letter|pleased to offer|job offer|we are delighted to offer|congratulations on your offer"
    );

    // ── OAuth token exchange ───────────────────────────────────────────────────

    /**
     * Exchanges an OAuth authorization code for a refresh token and persists it.
     */
    public void exchangeCodeAndSave(String code, String email) throws Exception {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new RuntimeException("Google OAuth not configured — add GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET to environment variables.");
        }

        HttpClient client = HttpClient.newHttpClient();
        String body = "code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var json = objectMapper.readTree(response.body());

        if (!json.has("refresh_token")) {
            log.error("Token exchange failed: {}", response.body());
            throw new RuntimeException("Failed to get refresh token — ensure prompt=consent was set in the auth URL.");
        }

        String refreshToken = json.get("refresh_token").asText();
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setGmailRefreshToken(refreshToken);
        user.setGmailConnected(true);
        userRepository.save(user);
        log.info("Gmail connected for user: {}", email);
    }

    // ── Main scan ─────────────────────────────────────────────────────────────

    /**
     * Fetches up to 50 recent job-related emails, classifies them, and
     * updates/creates applications accordingly.
     * @return count of emails that matched and were acted upon
     */
    public int scanEmails(String userEmail) throws Exception {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        if (user.getGmailRefreshToken() == null) {
            throw new RuntimeException("Gmail not connected. Please connect Gmail first.");
        }

        String accessToken = refreshAccessToken(user.getGmailRefreshToken());
        List<String> messageIds = fetchMessageIds(accessToken, 50);

        int detected = 0;
        for (String messageId : messageIds) {
            try {
                EmailData email = fetchEmailContent(accessToken, messageId);
                if (processEmail(email, user)) detected++;
            } catch (Exception e) {
                log.warn("Skipping email {}: {}", messageId, e.getMessage());
            }
        }

        user.setGmailLastScanned(LocalDateTime.now());
        userRepository.save(user);
        log.info("Gmail scan complete for {} — {} emails processed", userEmail, detected);
        return detected;
    }

    // ── Email processing ──────────────────────────────────────────────────────

    private boolean processEmail(EmailData email, User user) {
        String content = (email.subject() + " " + email.snippet()).toLowerCase();

        String detectedStatus = null;
        for (Map.Entry<String, String> entry : STATUS_PATTERNS.entrySet()) {
            boolean matches = Arrays.stream(entry.getValue().split("\\|"))
                    .anyMatch(content::contains);
            if (matches) {
                detectedStatus = entry.getKey();
                break;
            }
        }
        if (detectedStatus == null) return false;

        // Derive company name from email sender domain
        String companyName = extractCompanyName(email.from());
        if (companyName.equalsIgnoreCase("Unknown")) return false;

        // Try to find existing application for same company
        List<JobApplication> userApps = applicationRepository.findByUserId(user.getId());
        final String finalCompany = companyName;
        final String finalStatus  = detectedStatus;

        Optional<JobApplication> match = userApps.stream()
                .filter(app -> app.getCompanyName().toLowerCase().contains(finalCompany.toLowerCase())
                            || finalCompany.toLowerCase().contains(app.getCompanyName().toLowerCase()))
                .findFirst();

        if (match.isPresent()) {
            JobApplication app = match.get();
            // Only update status if it represents a progression, not a regression
            if (isProgression(app.getStatus(), finalStatus)) {
                String oldStatus = app.getStatus();
                app.setStatus(finalStatus);
                String notes = app.getNotes() == null ? "" : app.getNotes();
                app.setNotes(notes + "\n[Gmail Auto] " + email.subject() + " → " + finalStatus);
                applicationRepository.save(app);
                log.info("Auto-updated {} {} → {}", companyName, oldStatus, finalStatus);
            }
        } else if ("APPLIED".equals(finalStatus)) {
            // Auto-create a new application entry only for definitive confirmation emails
            JobApplication newApp = JobApplication.builder()
                    .user(user)
                    .companyName(companyName)
                    .jobTitle(extractJobTitle(email.subject()))
                    .status("APPLIED")
                    .source("EMAIL")
                    .appliedAt(LocalDate.now())
                    .isRemote(false)
                    .hasBondWarning(false)
                    .hasPaymentWarning(false)
                    .notes("[Gmail Auto-detected] " + email.subject())
                    .build();
            applicationRepository.save(newApp);
            log.info("Auto-created application from Gmail: {}", companyName);
        }
        return true;
    }

    /**
     * Prevents a status from being downgraded (e.g. OFFER → APPLIED).
     */
    private boolean isProgression(String current, String next) {
        List<String> order = List.of("SAVED", "APPLIED", "OA", "INTERVIEW", "OFFER", "REJECTED", "GHOSTED");
        int ci = order.indexOf(current);
        int ni = order.indexOf(next);
        // Allow rejected/offered regardless of current position; otherwise only allow forwards
        if (ni == order.indexOf("REJECTED") || ni == order.indexOf("OFFER")) return true;
        return ni > ci;
    }

    // ── Text extraction helpers ───────────────────────────────────────────────

    private String extractJobTitle(String subject) {
        return subject
                .replaceAll("(?i)re:|fwd:|your application|application for|application to|thank you for applying to", "")
                .replaceAll("(?i)at [A-Z][\\w ]+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractCompanyName(String fromHeader) {
        try {
            // "Noreply <no-reply@company.com>" → "company.com" → "company"
            String email = fromHeader.contains("<")
                    ? fromHeader.replaceAll(".*<(.+)>.*", "$1").trim()
                    : fromHeader.trim();
            String domain = email.split("@")[1];
            String base = domain.split("\\.")[0].toLowerCase();
            // Skip generic providers
            if (List.of("gmail", "yahoo", "hotmail", "outlook", "noreply", "no-reply", "mail", "notifications", "careers").contains(base)) {
                return "Unknown";
            }
            return Character.toUpperCase(base.charAt(0)) + base.substring(1);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ── Gmail API calls ───────────────────────────────────────────────────────

    private String refreshAccessToken(String refreshToken) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String body = "refresh_token=" + encode(refreshToken)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var json = objectMapper.readTree(response.body());
        if (!json.has("access_token")) {
            throw new RuntimeException("Failed to refresh access token: " + response.body());
        }
        return json.get("access_token").asText();
    }

    private List<String> fetchMessageIds(String accessToken, int maxResults) throws Exception {
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages"
                + "?maxResults=" + maxResults
                + "&q=subject:(application+OR+interview+OR+assessment+OR+offer+OR+rejected+OR+opportunity)";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        var json = objectMapper.readTree(response.body());

        List<String> ids = new ArrayList<>();
        if (json.has("messages")) {
            json.get("messages").forEach(m -> ids.add(m.get("id").asText()));
        }
        return ids;
    }

    private EmailData fetchEmailContent(String accessToken, String messageId) throws Exception {
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId
                + "?format=metadata&metadataHeaders=Subject&metadataHeaders=From";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        var json = objectMapper.readTree(response.body());

        String subject = "";
        String from    = "";
        var headers = json.path("payload").path("headers");
        for (var header : headers) {
            String name = header.get("name").asText();
            String value = header.get("value").asText();
            if ("Subject".equalsIgnoreCase(name)) subject = value;
            if ("From".equalsIgnoreCase(name))     from    = value;
        }
        String snippet = json.path("snippet").asText("");
        return new EmailData(subject, from, snippet, messageId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    public record EmailData(String subject, String from, String snippet, String messageId) {}
}
