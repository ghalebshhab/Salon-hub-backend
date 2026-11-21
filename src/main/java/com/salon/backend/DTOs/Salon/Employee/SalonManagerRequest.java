package com.salon.backend.DTOs.Salon.Employee;

/** True makes the employee a salon manager; false removes the role. */
public record SalonManagerRequest(Boolean manager) {
}
