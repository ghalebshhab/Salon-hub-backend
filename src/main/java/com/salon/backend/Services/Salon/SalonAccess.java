package com.salon.backend.Services.Salon;

import java.util.List;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.subscriptions.PlanFeature;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Subscription.PlanLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Who may run a salon's day-to-day work: its owner, or a salon manager.
 *
 * <p>A manager is an active employee of the salon whose owner gave them the
 * manager role, on a plan with {@link PlanFeature#MANAGER_ROLE}. When the
 * plan ends, the manager loses access at once; the flag stays, so access
 * comes back after an upgrade. Managers handle bookings and the team's
 * schedules only. Money, the plan, services, hiring and salon settings stay
 * with the owner, and those services keep their owner-only checks.
 */
@Component
@RequiredArgsConstructor
public class SalonAccess {

    private final SalonRepo salonRepo;
    private final UserRepo userRepo;
    private final PlanLimits planLimits;

    /**
     * The active salon this user may manage: the salon they own, or the
     * salon where they are a manager. Null when they may manage none.
     */
    public Salon managedSalon(Long userId) {
        if (userId == null) {
            return null;
        }
        Salon owned = salonRepo.findByOwnerId(userId);
        if (owned != null) {
            return isActiveSalon(owned) ? owned : null;
        }
        User user = userRepo.findById(userId).orElse(null);
        return isActiveManager(user) ? user.getSalon() : null;
    }

    /** The salon where this user is an active manager, or null. Owners get null. */
    public Salon managerSalon(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userRepo.findById(userId).orElse(null);
        return isActiveManager(user) ? user.getSalon() : null;
    }

    /** True when the user may manage this salon (owner or active manager). */
    public boolean canManage(Long userId, Salon salon) {
        if (salon == null || salon.getId() == null) {
            return false;
        }
        Salon managed = managedSalon(userId);
        return managed != null && salon.getId().equals(managed.getId());
    }

    /** True when the user is a manager who can use the role right now. */
    public boolean isActiveManager(User user) {
        if (user == null
                || !user.isSalonManager()
                || user.getRole() != UserRole.Employee
                || user.getStatus() != UserStatus.Active) {
            return false;
        }
        Salon salon = user.getSalon();
        return isActiveSalon(salon) && !planLimits.locked(salon, PlanFeature.MANAGER_ROLE);
    }

    /** The salon's managers who can use the role now; empty on other plans. */
    public List<User> activeManagers(Salon salon) {
        if (!isActiveSalon(salon) || planLimits.locked(salon, PlanFeature.MANAGER_ROLE)) {
            return List.of();
        }
        return userRepo.findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
                        salon.getId(), UserRole.Employee, UserStatus.Active)
                .stream()
                .filter(User::isSalonManager)
                .toList();
    }

    public static boolean isActiveSalon(Salon salon) {
        return salon != null
                && salon.getStatus() == SalonStatus.Accepted
                && !salon.isBlockedByAdmin()
                && !salon.isBlockedByOwner();
    }
}
