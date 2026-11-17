package com.salon.backend.Entities.community;

import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_post", indexes = {
        @Index(name = "idx_community_post_created", columnList = "created_at"),
        @Index(name = "idx_community_post_author", columnList = "author_id"),
        @Index(name = "idx_community_post_salon", columnList = "salon_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 1000)
    private String mediaUrl;

    /** Cloudinary id of an uploaded photo/video; null for a pasted link. */
    private String mediaPublicId;

    /** "image" or "video", needed to delete the file in Cloudinary. */
    private String mediaResourceType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
