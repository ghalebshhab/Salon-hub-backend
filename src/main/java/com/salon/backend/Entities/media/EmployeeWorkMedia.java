package com.salon.backend.Entities.media;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_work_media")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeWorkMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false, length = 1000)
    private String secureUrl;

    @Column(nullable = false, length = 255)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaResourceType resourceType;

    @Column(length = 300)
    private String caption;

    private Integer width;

    private Integer height;

    private Long bytes;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
