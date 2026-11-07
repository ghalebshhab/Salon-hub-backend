package com.salon.backend.Services.Activity;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserActivityDaily;
import com.salon.backend.Repositories.User.UserActivityDailyRepo;
import com.salon.backend.Repositories.User.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserActivityTrackingService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Amman");
    private static final Duration ACTIVITY_WRITE_INTERVAL = Duration.ofMinutes(1);

    private final UserRepo userRepo;
    private final UserActivityDailyRepo activityRepo;
    private final ConcurrentHashMap<Long, Instant> lastPersistedActivity = new ConcurrentHashMap<>();

    @Transactional
    public void recordSuccessfulLogin(User user) {
        if (user == null) return;
        Instant now = Instant.now();
        user.setLastLoginAt(now);
        user.setLastActivityAt(now);
        user.setTotalLoginCount(user.getTotalLoginCount() + 1);
        userRepo.save(user);

        UserActivityDaily daily = dailyRecord(user, now);
        daily.setLoginCount(daily.getLoginCount() + 1);
        daily.setActivityCount(daily.getActivityCount() + 1);
        daily.setLastSeenAt(now);
        activityRepo.save(daily);
        lastPersistedActivity.put(user.getId(), now);
    }

    @Transactional
    public void recordAuthenticatedActivity(Long userId) {
        if (userId == null || !shouldPersist(userId)) return;
        Instant now = Instant.now();
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        userRepo.updateLastActivityAt(userId, now);
        UserActivityDaily daily = dailyRecord(user, now);
        daily.setActivityCount(daily.getActivityCount() + 1);
        daily.setLastSeenAt(now);
        activityRepo.save(daily);
        lastPersistedActivity.put(userId, now);
    }

    private boolean shouldPersist(Long userId) {
        Instant now = Instant.now();
        Instant previous = lastPersistedActivity.get(userId);
        if (previous != null && Duration.between(previous, now).compareTo(ACTIVITY_WRITE_INTERVAL) < 0) return false;
        return previous == null
                ? lastPersistedActivity.putIfAbsent(userId, now) == null
                : lastPersistedActivity.replace(userId, previous, now);
    }

    private UserActivityDaily dailyRecord(User user, Instant now) {
        LocalDate date = now.atZone(BUSINESS_ZONE).toLocalDate();
        return activityRepo.findByUserIdAndActivityDate(user.getId(), date).orElseGet(() -> {
            UserActivityDaily daily = new UserActivityDaily();
            daily.setUser(user);
            daily.setActivityDate(date);
            daily.setFirstSeenAt(now);
            daily.setLastSeenAt(now);
            return daily;
        });
    }
}
