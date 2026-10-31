package com.salon.backend.Repositories.User;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Collection;

public interface UserRepo extends JpaRepository<User, Long> {
boolean existsByuserName(String username);
boolean existsByemail(String email);
boolean existsByphoneNumber(String phoneNumber);
User findByuserName(String username);
User findByemail(String email);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT user FROM User user WHERE user.email = :email")
java.util.Optional<User> findByEmailForUpdate(@Param("email") String email);

List<User> findByStatus(UserStatus status);
List<User> findBySalonIdAndRoleAndStatusOrderByFirstNameAscLastNameAsc(
        Long salonId,
        com.salon.backend.Entities.users.UserRole role,
        UserStatus status
);

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT user FROM User user WHERE user.id = :id")
java.util.Optional<User> findByIdForUpdate(@Param("id") Long id);

@Query(
        value = """
                SELECT DISTINCT user
                FROM User user
                LEFT JOIN user.careerSkills skill
                LEFT JOIN user.preferredWorkLocations location
                WHERE user.careerProfileEnabled = true
                  AND user.status = :status
                  AND user.role IN :roles
                  AND user.id <> :excludedUserId
                  AND (user.salon IS NULL OR user.salon.id <> :excludedSalonId)
                  AND (
                    :search = ''
                    OR LOWER(CONCAT(user.firstName, ' ', user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(user.userName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(skill) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(location) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                """,
        countQuery = """
                SELECT COUNT(DISTINCT user.id)
                FROM User user
                LEFT JOIN user.careerSkills skill
                LEFT JOIN user.preferredWorkLocations location
                WHERE user.careerProfileEnabled = true
                  AND user.status = :status
                  AND user.role IN :roles
                  AND user.id <> :excludedUserId
                  AND (user.salon IS NULL OR user.salon.id <> :excludedSalonId)
                  AND (
                    :search = ''
                    OR LOWER(CONCAT(user.firstName, ' ', user.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(user.userName) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(skill) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(location) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
                """
)
Page<User> searchCareerCandidates(
        @Param("excludedUserId") Long excludedUserId,
        @Param("excludedSalonId") Long excludedSalonId,
        @Param("status") UserStatus status,
        @Param("roles") Collection<com.salon.backend.Entities.users.UserRole> roles,
        @Param("search") String search,
        Pageable pageable
);

}
