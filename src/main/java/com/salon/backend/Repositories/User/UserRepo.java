package com.salon.backend.Repositories.User;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepo extends JpaRepository<User, Long> {
boolean existsByuserName(String username);
boolean existsByemail(String email);
boolean existsByphoneNumber(String phoneNumber);
User findByuserName(String username);
User findByemail(String email);
List<User> findByStatus(UserStatus status);

}
