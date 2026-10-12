package com.salon.backend.Entities.salons.services;

import com.salon.backend.Entities.users.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_service_capability",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_service_capability",
                columnNames = {"employee_id", "service_id"}
        ),
        indexes = {
                @Index(name = "idx_capability_employee", columnList = "employee_id"),
                @Index(name = "idx_capability_service", columnList = "service_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeServiceCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private SalonService service;

    private LocalDateTime assignedAt;
}
