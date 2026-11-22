package com.salon.backend.Services.Community;

import com.salon.backend.DTOs.Community.CreateCommentRequest;
import com.salon.backend.DTOs.Community.CreatePostRequest;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserRole;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Community.CommunityPostLikeRepo;
import com.salon.backend.Repositories.Community.FriendshipRepo;
import com.salon.backend.Repositories.Community.SalonFollowRepo;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Notification.CommunityNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CommunityServiceImpl.class)
@Transactional
class CommunityServiceImplTest {

    @Autowired private CommunityService service;
    @Autowired private UserRepo userRepo;
    @Autowired private SalonRepo salonRepo;
    @Autowired private CommunityPostLikeRepo likeRepo;
    @Autowired private FriendshipRepo friendshipRepo;
    @Autowired private SalonFollowRepo salonFollowRepo;
    @MockitoBean private CommunityNotificationService communityNotificationService;
    @MockitoBean private com.salon.backend.Services.Media.MediaStorageService mediaStorageService;

    @Test
    void createsLikesAndCommentsOnACommunityPost() {
        User author = user("maya");
        User visitor = user("sara");

        var created = service.createPost(
                new CreatePostRequest("  Fresh color work today  ", "https://cdn.example.com/work.jpg", null),
                author.getId());
        assertTrue(created.isSuccess());
        assertEquals("Fresh color work today", created.getData().content());

        var liked = service.likePost(created.getData().id(), visitor.getId());
        var likedAgain = service.likePost(created.getData().id(), visitor.getId());
        assertTrue(liked.isSuccess());
        assertTrue(likedAgain.getData().likedByMe());
        assertEquals(1, likeRepo.countByPostId(created.getData().id()));
        var likes = service.getPostLikes(created.getData().id(), author.getId());
        assertTrue(likes.isSuccess());
        assertEquals(visitor.getId(), likes.getData().get(0).user().id());

        var comment = service.addComment(created.getData().id(), new CreateCommentRequest(" Looks beautiful! "), visitor.getId());
        assertTrue(comment.isSuccess());
        assertEquals("Looks beautiful!", comment.getData().content());
    }

    @Test
    void exposesASafeCommunityProfileWithRelationshipContext() {
        User viewer = user("profile-viewer");
        User professional = user("profile-professional");
        professional.setCareerProfileEnabled(true);
        professional.setYearsOfExperience(6);
        professional.setCareerSkills(new java.util.ArrayList<>(java.util.List.of("Color", "Styling")));
        professional.setProfessionalBio("Color specialist");
        userRepo.save(professional);

        var profile = service.getCommunityProfile(professional.getId(), viewer.getId());

        assertTrue(profile.isSuccess());
        assertEquals(professional.getId(), profile.getData().member().user().id());
        assertEquals("NONE", profile.getData().member().relationship());
        assertEquals(java.util.List.of("Color", "Styling"), profile.getData().skills());
        assertEquals("Color specialist", profile.getData().professionalBio());
    }

    @Test
    void exposesOnlyTheRequestedMembersPostsWithCommunityCounts() {
        User viewer = user("member-post-viewer");
        User professional = user("member-post-author");
        service.createPost(new CreatePostRequest("First profile post", null, null), professional.getId());
        service.createPost(new CreatePostRequest("Second profile post", null, null), professional.getId());
        service.createPost(new CreatePostRequest("Someone else's post", null, null), viewer.getId());
        service.sendFriendRequest(professional.getId(), viewer.getId());
        var request = friendshipRepo.findByLowerUserIdAndHigherUserId(
                Math.min(viewer.getId(), professional.getId()),
                Math.max(viewer.getId(), professional.getId())).orElseThrow();
        service.acceptFriendRequest(request.getId(), professional.getId());

        var posts = service.getMemberPosts(professional.getId(), viewer.getId(), 0, 10);
        var profile = service.getCommunityProfile(professional.getId(), viewer.getId());

        assertTrue(posts.isSuccess());
        assertEquals(2, posts.getData().totalElements());
        assertTrue(posts.getData().content().stream()
                .allMatch(post -> post.author().id().equals(professional.getId())));
        assertEquals(2, profile.getData().postCount());
        assertEquals(1, profile.getData().friendCount());
    }

    @Test
    void protectsSalonIdentityAndKeepsFollowsIdempotent() {
        User owner = user("owner");
        User visitor = user("visitor");
        Salon salon = salon(owner);

        var rejected = service.createPost(new CreatePostRequest("Salon update", null, salon.getId()), visitor.getId());
        assertFalse(rejected.isSuccess());
        assertEquals("Only the salon owner can publish as this salon", rejected.getMessage());

        var published = service.createPost(new CreatePostRequest("Salon update", null, salon.getId()), owner.getId());
        assertTrue(published.isSuccess());
        assertEquals(salon.getId(), published.getData().salon().id());

        assertTrue(service.followSalon(salon.getId(), visitor.getId()).isSuccess());
        assertTrue(service.followSalon(salon.getId(), visitor.getId()).isSuccess());
        assertEquals(1, salonFollowRepo.findByUserIdOrderByCreatedAtDesc(visitor.getId()).size());
    }

    @Test
    void normalizesFriendPairsAndOnlyAllowsTheRecipientToAccept() {
        User requester = user("requester");
        User recipient = user("recipient");

        var request = service.sendFriendRequest(recipient.getId(), requester.getId());
        assertTrue(request.isSuccess());
        assertEquals(1, friendshipRepo.count());
        verify(communityNotificationService).friendRequestReceived(friendshipRepo.findById(request.getData().id()).orElseThrow());

        var duplicate = service.sendFriendRequest(requester.getId(), recipient.getId());
        assertFalse(duplicate.isSuccess());
        assertEquals("A friend request already exists", duplicate.getMessage());

        assertFalse(service.acceptFriendRequest(request.getData().id(), requester.getId()).isSuccess());
        assertTrue(service.acceptFriendRequest(request.getData().id(), recipient.getId()).isSuccess());
        assertEquals(1, service.getFriends(requester.getId()).getData().size());
        verify(communityNotificationService).friendRequestAccepted(friendshipRepo.findById(request.getData().id()).orElseThrow());
    }

    @Test
    void recipientCanDeclineAPendingFriendRequest() {
        User requester = user("decline-requester");
        User recipient = user("decline-recipient");
        var request = service.sendFriendRequest(recipient.getId(), requester.getId());
        var friendship = friendshipRepo.findById(request.getData().id()).orElseThrow();

        assertFalse(service.declineFriendRequest(friendship.getId(), requester.getId()).isSuccess());
        assertTrue(service.declineFriendRequest(friendship.getId(), recipient.getId()).isSuccess());
        verify(communityNotificationService).friendRequestDeclined(friendship);
        assertFalse(friendshipRepo.existsById(friendship.getId()));
    }

    @Test
    void postOwnerCanRemoveACommentFromTheirPost() {
        User author = user("author");
        User commenter = user("commenter");
        Long postId = service.createPost(new CreatePostRequest("My work", null, null), author.getId()).getData().id();
        Long commentId = service.addComment(postId, new CreateCommentRequest("Comment"), commenter.getId()).getData().id();

        assertTrue(service.deleteComment(postId, commentId, author.getId()).isSuccess());
        assertEquals(0, service.getComments(postId, author.getId(), 0, 20).getData().totalElements());
    }

    private User user(String userName) {
        User user = new User();
        user.setUserName(userName);
        user.setEmail(userName + "@example.com");
        user.setFirstName(userName);
        user.setLastName("User");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.Active);
        return userRepo.save(user);
    }

    private Salon salon(User owner) {
        Salon salon = new Salon();
        salon.setName("Glow Salon");
        salon.setOwner(owner);
        salon.setStatus(SalonStatus.Accepted);
        salon.setBlockedByAdmin(false);
        salon.setBlockedByOwner(false);
        return salonRepo.save(salon);
    }
}
