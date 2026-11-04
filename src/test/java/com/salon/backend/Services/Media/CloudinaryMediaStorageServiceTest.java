package com.salon.backend.Services.Media;

import com.salon.backend.Entities.media.MediaResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryMediaStorageServiceTest {
    // Non-credential test fixtures; all HTTP requests are intercepted by the mock server.
    private static final String TEST_KEY = "test-key";
    private static final String TEST_SECRET = "test-signing-value";
    private MockRestServiceServer server;
    private CloudinaryMediaStorageService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new CloudinaryMediaStorageService("test-cloud", TEST_KEY, TEST_SECRET, builder);
    }

    @Test
    void signedDeviceUploadMapsProviderMetadataWithoutReturningCredentials() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/auto/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    String timestamp = multipartField(body, "timestamp");
                    assertTrue(Long.parseLong(timestamp) > 0);
                    assertEquals("salonhub/employee-work", multipartField(body, "folder"));
                    assertEquals(TEST_KEY, multipartField(body, "api_key"));
                    assertEquals(sha1("folder=salonhub/employee-work&timestamp=" + timestamp + TEST_SECRET),
                            multipartField(body, "signature"));
                    assertTrue(body.contains("filename=\"work.png\""));
                    assertFalse(body.contains(TEST_SECRET));
                })
                .andRespond(withSuccess("""
                        {"secure_url":"https://res.cloudinary.com/test-cloud/image/upload/work.png",
                         "public_id":"salonhub/work", "resource_type":"image",
                         "width":1200,"height":800,"bytes":4500}
                        """, MediaType.APPLICATION_JSON));

        var response = service.upload(image(), "salonhub/employee-work");

        assertTrue(response.isSuccess());
        assertEquals("salonhub/work", response.getData().publicId());
        assertEquals(MediaResourceType.IMAGE, response.getData().resourceType());
        assertEquals(1200, response.getData().width());
        assertEquals(800, response.getData().height());
        assertEquals(4500L, response.getData().bytes());
        server.verify();
    }

    @Test
    void remoteDriveShareLinkUsesTheSameSignedUploadContract() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/auto/upload"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertEquals("https://drive.google.com/uc?export=download&id=abc_123",
                            multipartField(body, "file"));
                    assertEquals(sha1("folder=salonhub&timestamp=" + multipartField(body, "timestamp") + TEST_SECRET),
                            multipartField(body, "signature"));
                })
                .andRespond(withSuccess("""
                        {"secure_url":"https://res.cloudinary.com/test-cloud/video/upload/work.mp4",
                         "public_id":"salonhub/work", "resource_type":"video"}
                        """, MediaType.APPLICATION_JSON));

        var response = service.uploadRemote("https://drive.google.com/file/d/abc_123/view", null);

        assertTrue(response.isSuccess());
        assertEquals(MediaResourceType.VIDEO, response.getData().resourceType());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ok", "not found"})
    void deletionRequiresSignedRequestAndConfirmedOrAlreadyAbsentAsset(String result) {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/video/destroy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(request -> {
                    Map<String, String> form = formFields(((MockClientHttpRequest) request).getBodyAsString());
                    assertEquals("salonhub/work + original", form.get("public_id"));
                    assertEquals("true", form.get("invalidate"));
                    assertEquals(TEST_KEY, form.get("api_key"));
                    assertEquals(sha1("invalidate=true&public_id=salonhub/work + original&timestamp="
                            + form.get("timestamp") + TEST_SECRET), form.get("signature"));
                    assertFalse(form.containsValue(TEST_SECRET));
                })
                .andRespond(withSuccess("{\"result\":\"" + result + "\"}", MediaType.APPLICATION_JSON));

        assertTrue(service.delete("salonhub/work + original", "VIDEO").isSuccess());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"result\":\"pending\"}", "{\"error\":{\"message\":\"private provider detail\"}}", "not-json", ""})
    void ambiguousOrMalformedDeletionResponseMustNotReportSuccess(String body) {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/destroy"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        var response = service.delete("salonhub/work", "image");

        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertFalse(response.getMessage().contains("private provider detail"));
        server.verify();
    }

    @Test
    void providerHttpFailuresReturnSafeErrorsForUploadAndDeletion() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/auto/upload"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(TEST_SECRET));
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/image/destroy"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body(TEST_SECRET));

        var upload = service.upload(image(), "salonhub");
        var deletion = service.delete("salonhub/work", "image");

        assertFalse(upload.isSuccess());
        assertFalse(deletion.isSuccess());
        assertFalse(upload.getMessage().contains(TEST_SECRET));
        assertFalse(deletion.getMessage().contains(TEST_SECRET));
        server.verify();
    }

    @Test
    void missingConfigurationNeverReportsSuccessfulDeletionOrCallsProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer unconfiguredServer = MockRestServiceServer.bindTo(builder).build();
        var unconfigured = new CloudinaryMediaStorageService("", "", "", builder);

        assertFalse(unconfigured.isConfigured());
        assertFalse(unconfigured.upload(image(), "salonhub").isSuccess());
        assertFalse(unconfigured.uploadRemote("https://drive.google.com/file/d/abc/view", "salonhub").isSuccess());
        assertFalse(unconfigured.delete("salonhub/work", "image").isSuccess());
        unconfiguredServer.verify();
    }

    @Test
    void invalidStoredIdentifiersAndTypesCannotTriggerTheWrongDeletion() {
        assertFalse(service.delete(null, "image").isSuccess());
        assertFalse(service.delete(" ", "image").isSuccess());
        assertFalse(service.delete("salonhub/work", null).isSuccess());
        assertFalse(service.delete("salonhub/work", "raw").isSuccess());
        server.verify();
    }

    @Test
    void localValidationAndInvalidRemoteUrlsNeverCallProvider() {
        assertFalse(service.upload(null, "salonhub").isSuccess());
        assertFalse(service.upload(new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3, 4}), "salonhub").isSuccess());
        assertFalse(service.upload(new MockMultipartFile("file", "file.svg", "image/svg+xml", new byte[]{1}), "salonhub").isSuccess());
        assertFalse(service.upload(new MockMultipartFile("file", "large.png", "image/png", new byte[8 * 1024 * 1024 + 1]), "salonhub").isSuccess());
        assertFalse(service.uploadRemote("http://drive.google.com/file/d/abc/view", "salonhub").isSuccess());
        assertFalse(service.uploadRemote("https://example.com/work.png", "salonhub").isSuccess());
        server.verify();
    }

    @Test
    void missingUploadMetadataIsAnError() {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/auto/upload"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertFalse(service.upload(image(), "salonhub").isSuccess());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://drive.google.com/file/d/abc_123/view?usp=sharing",
            "https://docs.google.com/file/d/abc_123/preview",
            "https://DRIVE.GOOGLE.COM:443/file/d/abc_123/",
            "https://drive.google.com/open?id=abc_123&redirect=https%3A%2F%2F127.0.0.1",
            "https://drive.google.com/uc?export=download&id=abc_123&confirm=anything"
    })
    void acceptedDriveLinksDiscardUntrustedUrlComponents(String url) {
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/auto/upload"))
                .andExpect(request -> assertEquals("https://drive.google.com/uc?export=download&id=abc_123",
                        multipartField(((MockClientHttpRequest) request).getBodyAsString(), "file")))
                .andRespond(withSuccess("""
                        {"secure_url":"https://res.cloudinary.com/test-cloud/image/upload/work.png",
                         "public_id":"salonhub/work", "resource_type":"image"}
                        """, MediaType.APPLICATION_JSON));

        assertTrue(service.uploadRemote(url, "salonhub").isSuccess());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://drive.google.com:8443/file/d/abc/view",
            "https://drive.google.com:0/file/d/abc/view",
            "https://user:password@drive.google.com/file/d/abc/view",
            "https://drive.google.com.evil.example/file/d/abc/view",
            "https://evil.drive.google.com/file/d/abc/view",
            "https://lh3.googleusercontent.com/arbitrary",
            "https://googleusercontent.com/arbitrary",
            "https://drive.google.com/url?url=http://127.0.0.1",
            "https://docs.google.com/document/d/abc/edit",
            "https://drive.google.com/file/d/abc/view/extra",
            "https://drive.google.com/prefix/file/d/abc/view",
            "https://drive.google.com/file/d/abc/view#fragment",
            "https://drive.google.com/file/d/abc%2Fdef/view",
            "https://drive.google.com/open?id=abc&id=def",
            "https://drive.google.com/open?id=abc&%69d=def",
            "https://drive.google.com/open?id=",
            "https://drive.google.com/open?id=abc%26url%3Dhttp%3A%2F%2F127.0.0.1",
            "https://drive.google.com/uc?export=download",
            "https://127.0.0.1/file/d/abc/view",
            "https://[::1]/file/d/abc/view",
            "file:///etc/passwd",
            "not a URI"
    })
    void unsafeOrUnsupportedRemoteLinksNeverReachProvider(String url) {
        var response = service.uploadRemote(url, "salonhub");
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertTrue(response.getMessage().contains("Google Drive file share link"));
        server.verify();
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "work.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});
    }

    private String multipartField(String body, String name) {
        var matcher = Pattern.compile("name=\"" + name + "\"[^\\r\\n]*\\r\\n(?:[^\\r\\n]+\\r\\n)*\\r\\n([^\\r\\n]*)").matcher(body);
        assertTrue(matcher.find(), "Missing multipart field " + name);
        return matcher.group(1);
    }

    private Map<String, String> formFields(String body) {
        Map<String, String> fields = new HashMap<>();
        for (String part : body.split("&")) {
            String[] pair = part.split("=", 2);
            fields.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return fields;
    }

    private String sha1(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
