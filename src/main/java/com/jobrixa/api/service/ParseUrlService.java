package com.jobrixa.api.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;

/**
 * Fetches a job listing URL and extracts structured data using jsoup.
 * Uses platform-specific CSS selectors, with an OG/title fallback for unknown sites.
 */
@Slf4j
@Service
public class ParseUrlService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    public record ParsedJob(
            String companyName,
            String jobTitle,
            String location,
            String source,
            String jobUrl
    ) {}

    public ParsedJob parseUrl(String url) {
        String host = extractHost(url);
        String source = detectSource(host);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(8_000)
                    .followRedirects(true)
                    .get();

            String title = extractTitle(doc, source);
            String company = extractCompany(doc, source);
            String location = extractLocation(doc, source);

            // Final cleanup
            title   = clean(title);
            company = clean(company);
            location = clean(location);

            log.info("Parsed URL [{}] → title='{}', company='{}', source='{}'", url, title, company, source);
            return new ParsedJob(company, title, location, source, url);

        } catch (IOException e) {
            log.warn("Failed to fetch URL [{}]: {}", url, e.getMessage());
            // Return partial data so the frontend can still open the modal
            return new ParsedJob("", "", "", source, url);
        }
    }

    // ── Selectors per platform ─────────────────────────────────────────

    private String extractTitle(Document doc, String source) {
        return switch (source) {
            case "LINKEDIN"    -> text(doc, "h1.top-card-layout__title", "h1.topcard__title");
            case "INTERNSHALA" -> text(doc, ".profile_on_listing", "h1.heading_4_5");
            case "NAUKRI"      -> text(doc, "h1.title", "h1[class*='jd-header-title']");
            case "WELLFOUND"   -> text(doc, "h1[class*='title']", "h1");
            default            -> ogOrTitle(doc);
        };
    }

    private String extractCompany(Document doc, String source) {
        return switch (source) {
            case "LINKEDIN"    -> text(doc, "a.topcard__org-name-link", ".top-card-layout__card a.topcard__org-name-link");
            case "INTERNSHALA" -> text(doc, ".link_display_like_text", ".company_name");
            case "NAUKRI"      -> text(doc, ".jd-header-comp-name a", ".jd-header-comp-name");
            case "WELLFOUND"   -> text(doc, "a[class*='company']", "h2");
            default            -> ogSiteName(doc);
        };
    }

    private String extractLocation(Document doc, String source) {
        return switch (source) {
            case "LINKEDIN"    -> text(doc, ".top-card-layout__second-subline .topcard__flavor--bullet",
                                         ".top-card-layout__second-subline span");
            case "INTERNSHALA" -> text(doc, ".location_link", ".internship_other_details_container .item_body");
            case "NAUKRI"      -> text(doc, ".location-wrapper a", ".loc a");
            case "WELLFOUND"   -> text(doc, "[class*='location']");
            default            -> "";
        };
    }

    // ── Helpers ────────────────────────────────────────────────────────

    /** Try each selector in order, return first non-empty hit */
    private String text(Document doc, String... selectors) {
        for (String sel : selectors) {
            Element el = doc.selectFirst(sel);
            if (el != null && !el.text().isBlank()) return el.text().trim();
        }
        return "";
    }

    private String ogOrTitle(Document doc) {
        Element ogTitle = doc.selectFirst("meta[property='og:title']");
        if (ogTitle != null && !ogTitle.attr("content").isBlank()) {
            return ogTitle.attr("content").trim();
        }
        return doc.title();
    }

    private String ogSiteName(Document doc) {
        Element og = doc.selectFirst("meta[property='og:site_name']");
        return og != null ? og.attr("content").trim() : "";
    }

    private String detectSource(String host) {
        if (host.contains("linkedin"))    return "LINKEDIN";
        if (host.contains("internshala")) return "INTERNSHALA";
        if (host.contains("naukri"))      return "NAUKRI";
        if (host.contains("wellfound") || host.contains("angel")) return "WELLFOUND";
        if (host.contains("indeed"))      return "INDEED";
        if (host.contains("glassdoor"))   return "GLASSDOOR";
        if (host.contains("lever.co"))    return "LEVER";
        if (host.contains("greenhouse"))  return "GREENHOUSE";
        if (host.contains("workday"))     return "WORKDAY";
        return "OTHER";
    }

    private String extractHost(String url) {
        try {
            return URI.create(url).getHost().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    private String clean(String s) {
        if (s == null) return "";
        // Remove duplicate whitespace and trim
        return s.replaceAll("\\s+", " ").trim();
    }
}
