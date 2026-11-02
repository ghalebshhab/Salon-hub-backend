package com.salon.backend.Repositories.Media;

import com.salon.backend.Entities.media.EmployeeWorkMedia;
import com.salon.backend.Entities.media.MediaResourceType;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeWorkMediaRepoTest {
    @Autowired private EntityManager entityManager;
    @Autowired private EmployeeWorkMediaRepo repository;

    @Test
    void publicGalleryExcludesInactiveFormerTransferredAndNonEmployeePublishersBeforePagination() {
        Salon salon = salon();
        Salon otherSalon = salon();
        User active = employee(salon, UserRole.Employee, UserStatus.Active);
        EmployeeWorkMedia visible = media(salon, active);
        media(salon, employee(salon, UserRole.Employee, UserStatus.Blocked));
        media(salon, employee(salon, UserRole.Employee, UserStatus.Deleted));
        media(salon, employee(null, UserRole.Employee, UserStatus.Active));
        media(salon, employee(otherSalon, UserRole.Employee, UserStatus.Active));
        media(salon, employee(salon, UserRole.USER, UserStatus.Active));
        media(otherSalon, active);
        entityManager.flush();
        entityManager.clear();

        var result = repository.findPublicSalonWork(
                salon.getId(), UserRole.Employee, UserStatus.Active, PageRequest.of(0, 1));

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(visible.getId(), result.getContent().get(0).getId());
        assertFalse(result.hasNext());
    }

    @Test
    void membershipAndModerationChangesImmediatelyChangePublicVisibilityWithoutDeletingAssets() {
        Salon salon = salon();
        Salon destination = salon();
        User employee = employee(salon, UserRole.Employee, UserStatus.Active);
        media(salon, employee);
        entityManager.flush();
        assertEquals(1, visibleCount(salon));

        employee.setStatus(UserStatus.Blocked);
        entityManager.flush();
        assertEquals(0, visibleCount(salon));
        employee.setStatus(UserStatus.Active);
        entityManager.flush();
        assertEquals(1, visibleCount(salon));

        employee.setSalon(destination);
        entityManager.flush();
        assertEquals(0, visibleCount(salon));
        assertEquals(0, visibleCount(destination));
        assertEquals(1, repository.count());
    }

    @Test
    void equalTimestampsHaveStableNewestIdOrderAndAccuratePageCounts() {
        Salon salon = salon();
        User employee = employee(salon, UserRole.Employee, UserStatus.Active);
        EmployeeWorkMedia first = media(salon, employee);
        EmployeeWorkMedia second = media(salon, employee);
        EmployeeWorkMedia third = media(salon, employee);
        entityManager.flush();
        entityManager.clear();

        var page = repository.findPublicSalonWork(
                salon.getId(), UserRole.Employee, UserStatus.Active, PageRequest.of(0, 2));
        var next = repository.findPublicSalonWork(
                salon.getId(), UserRole.Employee, UserStatus.Active, PageRequest.of(1, 2));

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(java.util.List.of(third.getId(), second.getId()),
                page.getContent().stream().map(EmployeeWorkMedia::getId).toList());
        assertEquals(java.util.List.of(first.getId()),
                next.getContent().stream().map(EmployeeWorkMedia::getId).toList());
    }

    private long visibleCount(Salon salon) {
        return repository.findPublicSalonWork(
                salon.getId(), UserRole.Employee, UserStatus.Active, PageRequest.of(0, 1)).getTotalElements();
    }

    private Salon salon() {
        Salon salon = new Salon();
        salon.setStatus(SalonStatus.Accepted);
        entityManager.persist(salon);
        return salon;
    }

    private User employee(Salon salon, UserRole role, UserStatus status) {
        User employee = new User();
        employee.setSalon(salon);
        employee.setRole(role);
        employee.setStatus(status);
        entityManager.persist(employee);
        return employee;
    }

    private EmployeeWorkMedia media(Salon salon, User employee) {
        EmployeeWorkMedia media = new EmployeeWorkMedia();
        media.setSalon(salon);
        media.setEmployee(employee);
        media.setSecureUrl("https://res.cloudinary.com/test/image/upload/work.jpg");
        media.setPublicId("salonhub/test-work");
        media.setResourceType(MediaResourceType.IMAGE);
        media.setCreatedAt(LocalDateTime.of(2026, 11, 1, 12, 0));
        entityManager.persist(media);
        return media;
    }
}
