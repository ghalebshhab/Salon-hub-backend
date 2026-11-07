package com.salon.backend.Repositories.User;

import com.salon.backend.Entities.users.UserActivityDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserActivityDailyRepo extends JpaRepository<UserActivityDaily, Long> {
    Optional<UserActivityDaily> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);
    @EntityGraph(attributePaths = "user")
    List<UserActivityDaily> findByActivityDateGreaterThanEqual(LocalDate activityDate);
}
