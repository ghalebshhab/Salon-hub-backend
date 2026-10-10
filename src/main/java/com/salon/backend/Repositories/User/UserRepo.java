package com.salon.backend.Repositories.User;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface UserRepo extends JpaRepository<User, Long> {
boolean existsByuserName(String username);
boolean existsByemail(String email);
boolean existsByphoneNumber(String phoneNumber);
User findByuserName(String username);
User findByemail(String email);
List<User> findByStatus(UserStatus status);
List<User> findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
        Long salonId,
        com.salon.backend.Entities.users.UserRole role,
        UserStatus status
);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT user FROM User user WHERE user.id = :id")
java.util.Optional<User> findByIdForUpdate(@Param("id") Long id);

}
