package com.salon.backend.Services.Salon;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SalonMapLocationTest {

    @Test
    void buildsTheStoredLinkFromTheCoordinates() {
        SalonMapLocation.Result result = SalonMapLocation.resolve(
                "https://www.google.com/maps/place/Somewhere/@30.0,36.0,17z",
                31.95391234567,
                35.9106
        );

        assertThat(result.isValid()).isTrue();
        assertThat(result.latitude()).isEqualTo(31.9539123);
        assertThat(result.longitude()).isEqualTo(35.9106);
        assertThat(result.googleMapsUrl())
                .isEqualTo("https://www.google.com/maps/search/?api=1&query=31.9539123,35.9106");
    }

    @Test
    void acceptsCoordinatesWithoutALink() {
        SalonMapLocation.Result result = SalonMapLocation.resolve(null, 32.5556, 35.85);

        assertThat(result.isValid()).isTrue();
        assertThat(result.googleMapsUrl())
                .isEqualTo("https://www.google.com/maps/search/?api=1&query=32.5556,35.85");
    }

    @Test
    void clearsTheLocationWhenNothingIsSent() {
        SalonMapLocation.Result result = SalonMapLocation.resolve(" ", null, null);

        assertThat(result.isValid()).isTrue();
        assertThat(result.googleMapsUrl()).isNull();
        assertThat(result.latitude()).isNull();
        assertThat(result.longitude()).isNull();
    }

    @Test
    void rejectsALinkWithoutCoordinates() {
        SalonMapLocation.Result result =
                SalonMapLocation.resolve("https://maps.app.goo.gl/abc", null, null);

        assertThat(result.error()).contains("Select the salon location on the map");
    }

    @Test
    void rejectsOneCoordinateOnly() {
        assertThat(SalonMapLocation.resolve(null, 31.9, null).error())
                .isEqualTo("Latitude and longitude must be provided together");
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertThat(SalonMapLocation.resolve(null, Double.NaN, 35.9).error())
                .isEqualTo("The selected coordinates are not valid");
        assertThat(SalonMapLocation.resolve(null, 31.9, Double.POSITIVE_INFINITY).error())
                .isEqualTo("The selected coordinates are not valid");
    }

    @Test
    void rejectsCoordinatesOutsideJordan() {
        assertThat(SalonMapLocation.resolve(null, 33.9, 35.5).error())
                .isEqualTo("The selected salon location must be inside Jordan");
    }

    @Test
    void rejectsALinkThatIsNotGoogleMaps() {
        assertThat(SalonMapLocation.resolve("https://evil.example/maps", 31.9, 35.9).error())
                .isEqualTo("Enter a valid HTTPS Google Maps location URL");
        assertThat(SalonMapLocation.resolve("http://www.google.com/maps", 31.9, 35.9).error())
                .isEqualTo("Enter a valid HTTPS Google Maps location URL");
    }
}
