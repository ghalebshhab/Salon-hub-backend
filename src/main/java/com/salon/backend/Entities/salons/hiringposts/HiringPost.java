package com.salon.backend.Entities.salons.hiringposts;


import com.salon.backend.Entities.salons.Salon;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HiringPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    @ElementCollection
    @CollectionTable(
            name = "hiring_post_required_skills",
            joinColumns = @JoinColumn(name = "hiring_post_id")
    )
    @Column(name = "skill", nullable = false)
    private List<String> requiredSkills;

    private Integer minimumAge;

    private Integer maximumAge;

    private String employeeLocation;

    private Integer minimumYearsOfExperience;

    private Integer numOfPositions;

    @Enumerated(EnumType.STRING)
    private HiringPostStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

}
