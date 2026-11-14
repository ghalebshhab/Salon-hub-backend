package com.salon.backend.Repositories.User;

import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Services.Community.CommunityServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CommunityUserSearchRepoTest {

    @Autowired private EntityManager entityManager;
    @Autowired private UserRepo userRepo;

    private User viewer;

    @BeforeEach
    void seed() {
        viewer = user("Viewer", "Person", "viewer", UserStatus.Active);
        user("Rania", "Haddad", "rania.h", UserStatus.Active);
        user("Rami", "Saleh", "rsaleh", UserStatus.Active);
        user("Omar", "Rashid", "omar_r", UserStatus.Active);
        user("Lina", "Khoury", "lina100%", UserStatus.Active);
        user("Rasha", "Blocked", "rasha", UserStatus.Blocked);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void firstCharacterReturnsEveryActiveMemberWithAMatchingNameStart() {
        assertThat(search("r")).containsExactly("Omar Rashid", "Rami Saleh", "Rania Haddad");
    }

    @Test
    void eachExtraCharacterNarrowsTheResults() {
        assertThat(search("ra")).containsExactly("Omar Rashid", "Rami Saleh", "Rania Haddad");
        assertThat(search("ran")).containsExactly("Rania Haddad");
        assertThat(search("rania h")).containsExactly("Rania Haddad");
    }

    @Test
    void matchingIgnoresLetterCase() {
        assertThat(search("RAM")).containsExactly("Rami Saleh");
        assertThat(search("hAdD")).containsExactly("Rania Haddad");
    }

    @Test
    void matchesOnlyAtTheStartOfNamesNotInTheMiddle() {
        assertThat(search("ad")).isEmpty();
        assertThat(search("ania")).isEmpty();
    }

    @Test
    void matchesUsernamePrefixes() {
        assertThat(search("rsal")).containsExactly("Rami Saleh");
    }

    @Test
    void typedWildcardsMatchLiterally() {
        assertThat(search("%")).isEmpty();
        assertThat(search("_")).isEmpty();
        assertThat(search("lina100%")).containsExactly("Lina Khoury");
        assertThat(search("omar_")).containsExactly("Omar Rashid");
    }

    private List<String> search(String text) {
        return userRepo.searchCommunityUsers(
                        viewer.getId(), UserStatus.Active, CommunityServiceImpl.likePrefix(text),
                        PageRequest.of(0, 30, Sort.by("firstName", "lastName").ascending()))
                .getContent().stream()
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .toList();
    }

    private User user(String first, String last, String userName, UserStatus status) {
        User user = new User();
        user.setFirstName(first);
        user.setLastName(last);
        user.setUserName(userName);
        user.setEmail(userName.replaceAll("[^a-z0-9]", "") + "@example.test");
        user.setRole(UserRole.USER);
        user.setStatus(status);
        entityManager.persist(user);
        return user;
    }
}
