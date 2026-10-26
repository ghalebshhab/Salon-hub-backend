package com.salon.backend.DTOs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void errorAlwaysContainsAClearNonBlankMessage() {
        ApiResponse<Void> response = ApiResponse.error("   ");

        assertFalse(response.isSuccess());
        assertEquals(
                "The request could not be completed. Please review the submitted information and try again.",
                response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorTrimsTheProvidedBusinessReason() {
        ApiResponse<Void> response = ApiResponse.error("  Booking time is no longer available.  ");

        assertEquals("Booking time is no longer available.", response.getMessage());
    }
}
