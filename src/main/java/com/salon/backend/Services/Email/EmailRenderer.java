package com.salon.backend.Services.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Renders an EmailTemplate into SalonHub-branded HTML (table layout and
 * inline styles, so it works in Gmail, Outlook and phone mail apps) and a
 * plain-text copy. All user content is HTML-escaped.
 */
@Component
public class EmailRenderer {

    private static final String PLUM = "#76546f";
    private static final String PLUM_SOFT = "#f1e8ef";
    private static final String GOLD = "#d7a86e";
    private static final String INK = "#292329";
    private static final String BODY = "#4a4248";
    private static final String MUTED = "#746b72";
    private static final String CANVAS = "#f6f1ee";
    private static final String LINE = "#e8dedf";

    private final String frontendUrl;

    public EmailRenderer(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        String url = frontendUrl == null ? "" : frontendUrl.trim();
        this.frontendUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public EmailContent render(EmailTemplate template, String recipientName) {
        return new EmailContent(template.subject(), html(template, recipientName), text(template, recipientName));
    }

    String absoluteUrl(String path) {
        if (path == null || path.isBlank()) return frontendUrl;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return frontendUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String html(EmailTemplate t, String name) {
        StringBuilder body = new StringBuilder();
        body.append(p(greeting(name)));
        t.paragraphs().forEach(paragraph -> body.append(p(paragraph)));

        if (t.code() != null) {
            body.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px 0 20px\"><tr><td align=\"center\" style=\"padding:18px;border-radius:12px;background:")
                    .append(PLUM_SOFT).append("\"><div style=\"font-family:'SFMono-Regular',Consolas,'Courier New',monospace;font-size:30px;font-weight:700;letter-spacing:10px;color:")
                    .append(PLUM).append("\">").append(esc(t.code())).append("</div></td></tr></table>");
        }

        if (!t.details().isEmpty()) {
            body.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:4px 0 20px;border:1px solid ")
                    .append(LINE).append(";border-radius:12px;border-collapse:separate\">");
            for (int i = 0; i < t.details().size(); i++) {
                EmailTemplate.Detail detail = t.details().get(i);
                String border = i == 0 ? "" : "border-top:1px solid " + LINE + ";";
                body.append("<tr><td style=\"").append(border).append("padding:10px 14px;font-size:13px;color:").append(MUTED)
                        .append(";width:38%\">").append(esc(detail.label())).append("</td><td style=\"").append(border)
                        .append("padding:10px 14px;font-size:14px;font-weight:600;color:").append(INK).append("\">")
                        .append(esc(detail.value())).append("</td></tr>");
            }
            body.append("</table>");
        }

        if (t.ctaLabel() != null) {
            body.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:6px 0 22px\"><tr><td style=\"border-radius:10px;background:")
                    .append(PLUM).append("\"><a href=\"").append(esc(absoluteUrl(t.ctaPath())))
                    .append("\" style=\"display:inline-block;padding:13px 24px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px\">")
                    .append(esc(t.ctaLabel())).append("</a></td></tr></table>");
        }

        if (t.footnote() != null) {
            body.append("<p style=\"margin:0 0 4px;font-size:13px;line-height:1.6;color:").append(MUTED).append("\">")
                    .append(esc(t.footnote())).append("</p>");
        }

        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"color-scheme\" content=\"light\"><title>" + esc(t.subject()) + "</title></head>"
                + "<body style=\"margin:0;padding:0;background:" + CANVAS + "\">"
                // Preheader: the preview line in the inbox list.
                + "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0\">" + esc(t.preheader()) + "</div>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:" + CANVAS + "\"><tr><td align=\"center\" style=\"padding:28px 14px\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px\">"
                + "<tr><td style=\"padding:0 4px 14px;font-family:Georgia,'Times New Roman',serif;font-size:22px;font-weight:700;color:" + PLUM + "\">"
                + "Salon<span style=\"color:" + GOLD + ";font-style:italic\">hub</span></td></tr>"
                + "<tr><td style=\"background:#ffffff;border:1px solid " + LINE + ";border-top:4px solid " + PLUM + ";border-radius:16px;padding:30px 28px;"
                + "font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif\">"
                + "<h1 style=\"margin:0 0 16px;font-family:Georgia,'Times New Roman',serif;font-size:23px;line-height:1.3;font-weight:700;color:" + INK + "\">"
                + esc(t.heading()) + "</h1>"
                + body
                + "</td></tr>"
                + "<tr><td style=\"padding:18px 6px 0;font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;font-size:12px;line-height:1.6;color:" + MUTED + "\">"
                + esc(footer(t.priority())) + "<br><a href=\"" + esc(frontendUrl) + "\" style=\"color:" + PLUM + "\">SalonHub</a> · Made for Jordan"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private String text(EmailTemplate t, String name) {
        StringBuilder out = new StringBuilder();
        out.append(t.heading()).append("\n\n").append(greeting(name)).append("\n\n");
        t.paragraphs().forEach(paragraph -> out.append(paragraph).append("\n\n"));
        if (t.code() != null) out.append("Code: ").append(t.code()).append("\n\n");
        t.details().forEach(detail -> out.append(detail.label()).append(": ").append(detail.value()).append('\n'));
        if (!t.details().isEmpty()) out.append('\n');
        if (t.ctaLabel() != null) out.append(t.ctaLabel()).append(": ").append(absoluteUrl(t.ctaPath())).append("\n\n");
        if (t.footnote() != null) out.append(t.footnote()).append("\n\n");
        out.append("— SalonHub\n").append(footer(t.priority()));
        return out.toString();
    }

    private String footer(EmailPriority priority) {
        return priority == EmailPriority.CRITICAL
                ? "This is a security email about your SalonHub account."
                : "You get this email because you have a SalonHub account.";
    }

    private String p(String value) {
        return "<p style=\"margin:0 0 14px;font-size:15px;line-height:1.6;color:" + BODY + "\">" + esc(value) + "</p>";
    }

    private String greeting(String name) {
        return name == null || name.isBlank() ? "Hello," : "Hello " + name.trim() + ",";
    }

    static String esc(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
