package com.salon.backend.Utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlValidationUtilsTest {

    @Test
    void acceptsHttpAndHttpsPortfolioUrls() {
        assertThat(UrlValidationUtils.isValidWebUrl("https://portfolio.example.com/user"))
                .isTrue();
        assertThat(UrlValidationUtils.isValidWebUrl("http://example.com/portfolio?id=7"))
                .isTrue();
    }

    @Test
    void rejectsInvalidOrUnsupportedPortfolioUrls() {
        assertThat(UrlValidationUtils.isValidWebUrl("portfolio.example.com/user"))
                .isFalse();
        assertThat(UrlValidationUtils.isValidWebUrl("ftp://example.com/portfolio"))
                .isFalse();
        assertThat(UrlValidationUtils.isValidWebUrl("not a url"))
                .isFalse();
    }

    @Test
    void acceptsOnlyHttpOrHttpsPdfResumeUrls() {
        assertThat(UrlValidationUtils.isValidPdfUrl("https://files.example.com/resume.PDF"))
                .isTrue();
        assertThat(UrlValidationUtils.isValidPdfUrl(
                "https://files.example.com/resume.pdf?download=true"
        )).isTrue();
        assertThat(UrlValidationUtils.isValidPdfUrl("https://files.example.com/resume.docx"))
                .isFalse();
        assertThat(UrlValidationUtils.isValidPdfUrl("ftp://files.example.com/resume.pdf"))
                .isFalse();
    }
}
