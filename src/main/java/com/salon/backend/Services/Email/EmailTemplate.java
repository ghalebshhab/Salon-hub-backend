package com.salon.backend.Services.Email;

import java.util.ArrayList;
import java.util.List;

/**
 * The content of one email, independent of HTML. EmailRenderer turns it into
 * the branded HTML and plain-text versions.
 *
 * @param code       a one-time code shown large (security emails), or null
 * @param details    label/value rows such as salon, date and time
 * @param ctaPath    a frontend path such as "/my-bookings"; made absolute
 */
public record EmailTemplate(
        EmailPriority priority,
        String subject,
        String preheader,
        String heading,
        List<String> paragraphs,
        String code,
        List<Detail> details,
        String ctaLabel,
        String ctaPath,
        String footnote
) {

    public record Detail(String label, String value) {}

    public static Builder builder(EmailPriority priority, String subject) {
        return new Builder(priority, subject);
    }

    public static final class Builder {
        private final EmailPriority priority;
        private final String subject;
        private String preheader;
        private String heading;
        private final List<String> paragraphs = new ArrayList<>();
        private String code;
        private final List<Detail> details = new ArrayList<>();
        private String ctaLabel;
        private String ctaPath;
        private String footnote;

        private Builder(EmailPriority priority, String subject) {
            this.priority = priority;
            this.subject = subject;
        }

        public Builder preheader(String value) { this.preheader = value; return this; }

        public Builder heading(String value) { this.heading = value; return this; }

        public Builder paragraph(String value) {
            if (value != null && !value.isBlank()) paragraphs.add(value.trim());
            return this;
        }

        public Builder code(String value) { this.code = value; return this; }

        public Builder detail(String label, String value) {
            if (value != null && !value.isBlank()) details.add(new Detail(label, value.trim()));
            return this;
        }

        public Builder cta(String label, String path) {
            this.ctaLabel = label;
            this.ctaPath = path;
            return this;
        }

        public Builder footnote(String value) { this.footnote = value; return this; }

        public EmailTemplate build() {
            return new EmailTemplate(priority, subject,
                    preheader == null ? heading : preheader,
                    heading == null ? subject : heading,
                    List.copyOf(paragraphs), code, List.copyOf(details),
                    ctaLabel, ctaPath, footnote);
        }
    }
}
