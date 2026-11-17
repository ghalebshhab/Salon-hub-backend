package com.salon.backend.Entities.users;


import com.salon.backend.Entities.salons.Salon;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String firstName;

    private String lastName;

    private String email;

    private String userName;

    private String password;

    private String phoneNumber;

    // Null is treated as verified for accounts created before email verification existed.
    private Boolean emailVerified = Boolean.TRUE;

    private Instant credentialsUpdatedAt;

    private Instant lastLoginAt;

    private Instant lastActivityAt;

    @Column(nullable = false)
    private long totalLoginCount;

    private UserRole role;

    private UserStatus status;

    @ManyToOne
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(nullable = false)
    private boolean careerProfileEnabled;

    private LocalDate dateOfBirth;

    private Integer yearsOfExperience;

    @ElementCollection
    @CollectionTable(
            name = "user_career_skills",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "skill", nullable = false)
    private List<String> careerSkills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "user_preferred_work_locations",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "location", nullable = false)
    private List<String> preferredWorkLocations = new ArrayList<>();

    private String resumeUrl;

    private String portfolioUrl;

    @Column(length = 1000)
    private String profilePictureUrl;

    private String profilePicturePublicId;

    @Column(length = 2000)
    private String professionalBio;


}
