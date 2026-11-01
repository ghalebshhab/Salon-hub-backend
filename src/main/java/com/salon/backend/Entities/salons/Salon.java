package com.salon.backend.Entities.salons;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String name;

    // this is the salon phone , not the owner .
    private String salonPhoneNumber;

    private String ownerPhoneNumber;
    // also this is the salon email .
    private String email;

    private String location;

    @Column(length = 1000)
    private String googleMapsUrl;

    private Double latitude;

    private Double longitude;
    // as a salon services description  , about the salon .
    private String description;

    @OneToOne(cascade = CascadeType.ALL)
    private User owner;

    @OneToMany(mappedBy = "salon")
    private List<User> employees;

    private Long currentEmployeesNumber;

    private Long maxEmployeesNumber;

    private LocalDate createdAt;

    private LocalTime openingTime;

    private LocalTime closingTime;

    private String profilePictureUrl;

    private String profilePicturePublicId;

    private SalonStatus status;

    @Enumerated(EnumType.STRING)
    private SalonStatus statusBeforeBlock;

    @Column(nullable = false)
    private boolean blockedByAdmin;

    @Column(nullable = false)
    private boolean blockedByOwner;

    @Enumerated(EnumType.STRING)
    private UserRole ownerRoleBeforeAdminBlock;

    private SalonOpenningStatus openningStatus;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(nullable = false)
    private int resubmissionCount;
}
