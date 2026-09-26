package com.salon.backend.Entities.salons.hiringposts;

import com.salon.backend.Entities.salons.employment.EmploymentRequest;
import com.salon.backend.Entities.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hiring_post_applicant",
                columnNames = {"hiring_post_id", "applicant_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HiringApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hiring_post_id", nullable = false)
    private HiringPost hiringPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_request_id", nullable = false, unique = true)
    private EmploymentRequest employmentRequest;

    private LocalDate dateOfBirth;

    private String currentLocation;

    private Integer yearsOfExperience;

    @ElementCollection
    @CollectionTable(
            name = "hiring_application_skills",
            joinColumns = @JoinColumn(name = "hiring_application_id")
    )
    @Column(name = "skill", nullable = false)
    private List<String> skills;

    private String resumeUrl;

    private String portfolioUrl;

    @Column(length = 2000)
    private String applicationNote;

    private LocalDateTime createdAt;
}
