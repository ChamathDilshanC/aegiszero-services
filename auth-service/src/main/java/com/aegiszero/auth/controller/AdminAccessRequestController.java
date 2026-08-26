package com.aegiszero.auth.controller;

import com.aegiszero.auth.service.AdminAccessRequestService;
import com.aegiszero.auth.service.AdminAccessRequestService.Decision;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Serves the one-click Approve/Reject links from the admin-access-request
 * email. A human clicks these straight out of their inbox, so the response
 * is a small standalone HTML page (not JSON) — there is no SPA route for
 * this and none is needed.
 */
@RestController
@RequestMapping("/api/auth/admin-requests")
@RequiredArgsConstructor
public class AdminAccessRequestController {

    private final AdminAccessRequestService service;

    @GetMapping(value = "/{requestId}/approve", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> approve(@PathVariable UUID requestId, @RequestParam String token) {
        return render(service.approve(requestId, token));
    }

    @GetMapping(value = "/{requestId}/reject", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> reject(@PathVariable UUID requestId, @RequestParam String token) {
        return render(service.reject(requestId, token));
    }

    private ResponseEntity<String> render(Decision decision) {
        String name = decision.request() != null && decision.request().getFirstName() != null
                ? decision.request().getFirstName() : "there";
        String email = decision.request() != null ? decision.request().getEmail() : null;

        record Page(HttpStatus status, String icon, String title, String body) {
        }

        Page page = switch (decision.outcome()) {
            case APPROVED -> new Page(HttpStatus.OK, "check",
                    "Admin access approved",
                    "%s (%s) now has the ADMIN role and can sign in with full admin access.".formatted(name, email));
            case REJECTED -> new Page(HttpStatus.OK, "cross",
                    "Request declined",
                    "The admin access request from %s (%s) has been declined. Their account remains a standard user.".formatted(name, email));
            case ALREADY_DECIDED -> new Page(HttpStatus.CONFLICT, "info",
                    "Already handled",
                    "This request was already decided earlier — no action was taken.");
            case EXPIRED -> new Page(HttpStatus.GONE, "clock",
                    "Link expired",
                    "This approval link has expired. Ask %s to register again to send a new request.".formatted(name));
            case INVALID_TOKEN, NOT_FOUND -> new Page(HttpStatus.NOT_FOUND, "cross",
                    "Invalid link",
                    "This link is invalid, malformed, or has already been used.");
            case GRANT_FAILED -> new Page(HttpStatus.INTERNAL_SERVER_ERROR, "cross",
                    "Couldn't grant access",
                    "Approval was received but granting the role failed. Please try the link again shortly, or assign the role manually from the console.");
        };

        return ResponseEntity.status(page.status())
                .contentType(MediaType.TEXT_HTML)
                .body(html(page.icon(), page.title(), page.body()));
    }

    private String html(String icon, String title, String body) {
        String glyph = switch (icon) {
            case "check" -> "&#10003;";
            case "clock" -> "&#8987;";
            case "info" -> "&#8505;";
            default -> "&#10005;";
        };
        return """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s · AegisZero</title>
                <style>
                  :root { color-scheme: dark; }
                  * { box-sizing: border-box; }
                  body {
                    margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
                    background: #0a0a0a; background-image: radial-gradient(circle at 20%% 20%%, rgba(67,0,255,0.25), transparent 45%%), radial-gradient(circle at 80%% 80%%, rgba(0,255,222,0.15), transparent 45%%);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, Roboto, sans-serif;
                    color: #f5f5f7; padding: 24px;
                  }
                  .card {
                    max-width: 420px; width: 100%%; text-align: center; padding: 40px 32px;
                    border-radius: 20px; border: 1px solid rgba(255,255,255,0.08);
                    background: rgba(255,255,255,0.04); backdrop-filter: blur(12px);
                    box-shadow: 0 20px 60px -20px rgba(0,0,0,0.6);
                  }
                  .badge {
                    width: 56px; height: 56px; margin: 0 auto 20px; border-radius: 16px;
                    display: flex; align-items: center; justify-content: center; font-size: 26px;
                    background: linear-gradient(135deg, #4300ff, #00caff); color: #fff;
                  }
                  h1 { font-size: 20px; margin: 0 0 12px; letter-spacing: -0.01em; }
                  p { font-size: 14px; line-height: 1.6; color: #b8b8c2; margin: 0; }
                  .brand { margin-top: 32px; font-size: 12px; color: #6b6b76; letter-spacing: 0.04em; text-transform: uppercase; }
                </style>
                </head>
                <body>
                  <div class="card">
                    <div class="badge">%s</div>
                    <h1>%s</h1>
                    <p>%s</p>
                    <div class="brand">AegisZero</div>
                  </div>
                </body>
                </html>
                """.formatted(title, glyph, title, body);
    }
}
