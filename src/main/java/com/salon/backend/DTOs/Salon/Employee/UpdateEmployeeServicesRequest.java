package com.salon.backend.DTOs.Salon.Employee;

import java.util.List;

public record UpdateEmployeeServicesRequest(List<Long> serviceIds) {
}
