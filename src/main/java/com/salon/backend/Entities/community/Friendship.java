package com.salon.backend.Entities.community;

import com.salon.backend.Entities.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendship", uniqueConstraints = @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"lower_user_id", "higher_user_id"}), indexes = {
        @Index(name = "idx_friendship_requester", columnList = "requester_id,status"),
        @Index(name = "idx_friendship_addressee", columnList = "addressee_id,status")
})
@Getter
@Setter
@NoArgsConstructor
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Column(name = "lower_user_id", nullable = false, updatable = false)
    private Long lowerUserId;

    @Column(name = "higher_user_id", nullable = false, updatable = false)
    private Long higherUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendshipStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
}
