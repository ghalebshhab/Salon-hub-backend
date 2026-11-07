package com.salon.backend.Services.Activity;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Auth.Dashboard.UserActivityDashboardResponse;
import com.salon.backend.DTOs.Auth.Dashboard.UserActivityResponse;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserActivityDaily;
import com.salon.backend.Repositories.User.UserActivityDailyRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Auth.Security.AdminAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserActivityServiceImpl implements AdminUserActivityService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Amman");
    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final UserRepo userRepo;
    private final UserActivityDailyRepo activityRepo;
    private final AdminAuthorizationService adminAuthorizationService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserActivityDashboardResponse> getUserActivity(Long adminId) {
        ApiResponse<User> authorization = adminAuthorizationService.authorize(adminId);
        if (!authorization.isSuccess()) return ApiResponse.error(authorization.getMessage());

        Instant now = Instant.now();
        LocalDate today = now.atZone(BUSINESS_ZONE).toLocalDate();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        Map<Long, List<UserActivityDaily>> activityByUser = activityRepo
                .findByActivityDateGreaterThanEqual(yearStart)
                .stream()
                .collect(Collectors.groupingBy(activity -> activity.getUser().getId()));

        List<UserActivityResponse> rows = userRepo.findAll().stream()
                .map(user -> mapUser(user, activityByUser.getOrDefault(user.getId(), List.of()), now, today, weekStart, monthStart, yearStart))
                .sorted(Comparator.comparing(UserActivityResponse::online).reversed()
                        .thenComparing(UserActivityResponse::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long online = rows.stream().filter(UserActivityResponse::online).count();
        long activeToday = rows.stream().filter(row -> row.activityToday() > 0).count();
        long activeWeek = rows.stream().filter(row -> row.activityThisWeek() > 0).count();
        long activeMonth = rows.stream().filter(row -> row.activityThisMonth() > 0).count();
        return ApiResponse.success("User activity fetched successfully", new UserActivityDashboardResponse(
                rows.size(), online, activeToday, activeWeek, activeMonth, now, rows
        ));
    }

    private UserActivityResponse mapUser(User user, List<UserActivityDaily> activity, Instant now, LocalDate today, LocalDate weekStart, LocalDate monthStart, LocalDate yearStart) {
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (fullName.isBlank()) fullName = user.getUserName();
        Instant lastActivity = user.getLastActivityAt();
        boolean online = lastActivity != null && !lastActivity.isBefore(now.minus(ONLINE_WINDOW));
        return new UserActivityResponse(
                user.getId(), fullName, user.getUserName(), user.getEmail(), user.getRole(), user.getStatus(), online,
                user.getLastLoginAt(), lastActivity, user.getTotalLoginCount(),
                sum(activity, today, true), sum(activity, weekStart, true), sum(activity, monthStart, true), sum(activity, yearStart, true),
                sum(activity, today, false), sum(activity, weekStart, false), sum(activity, monthStart, false), sum(activity, yearStart, false)
        );
    }

    private long sum(List<UserActivityDaily> rows, LocalDate start, boolean logins) {
        return rows.stream().filter(row -> !row.getActivityDate().isBefore(start))
                .mapToLong(row -> logins ? row.getLoginCount() : row.getActivityCount()).sum();
    }
}
