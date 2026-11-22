package com.salon.backend.DTOs.Salon;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSalonRequest {

    private String salonName;
    private String salonDescription;
    /** Optional. Blank means "translate for me"; absent keeps the saved Arabic. */
    private String salonDescriptionAr;
    private String salonPhoneNumber;
    private String salonEmail;
    private String salonCity;
    private String googleMapsUrl;
    private Double latitude;
    private Double longitude;
    private Long maxNumberOfEmployees;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String profilePictureUrl;
}
