package com.salon.backend.Services.Community;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Community.*;
import com.salon.backend.Entities.community.*;
import com.salon.backend.Entities.salons.Salon;
import com.salon.backend.Entities.salons.SalonStatus;
import com.salon.backend.Entities.users.User;
import com.salon.backend.Entities.users.UserStatus;
import com.salon.backend.Repositories.Community.*;
import com.salon.backend.Repositories.Salon.SalonRepo;
import com.salon.backend.Repositories.User.UserRepo;
import com.salon.backend.Services.Notification.CommunityNotificationService;
import com.salon.backend.Utils.UrlValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_POST_LENGTH = 5000;
    private static final int MAX_COMMENT_LENGTH = 2000;

    private final CommunityPostRepo postRepo;
    private final CommunityCommentRepo commentRepo;
    private final CommunityPostLikeRepo likeRepo;
    private final FriendshipRepo friendshipRepo;
    private final SalonFollowRepo salonFollowRepo;
    private final UserRepo userRepo;
    private final SalonRepo salonRepo;
    private final CommunityNotificationService communityNotificationService;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CommunityMemberResponse>> searchMembers(Long userId, String search, Integer size) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        int resultSize = size == null ? 12 : size;
        if (resultSize < 1 || resultSize > 30) return ApiResponse.error("Member result size must be between 1 and 30");
        String query = normalize(search);
        List<CommunityMemberResponse> members = userRepo.searchCommunityUsers(
                        userId, UserStatus.Active, query == null ? "" : likePrefix(query),
                        PageRequest.of(0, resultSize, Sort.by("firstName", "lastName").ascending()))
                .getContent().stream().map(member -> mapMember(member, userId)).toList();
        return ApiResponse.success("Community members fetched successfully", members);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CommunityPostPageResponse> getFeed(Long userId, Integer page, Integer size) {
        User user = findActiveUser(userId);
        if (user == null) return ApiResponse.error("Active user not found");
        PageRequest pageRequest = pageRequest(page, size);
        if (pageRequest == null) return ApiResponse.error("Page must be non-negative and size must be between 1 and 50");

        Page<CommunityPost> posts = postRepo.findByAuthorStatus(UserStatus.Active, pageRequest);
        Set<Long> likedPostIds = likedPostIds(posts.getContent(), userId);
        List<CommunityPostResponse> content = posts.getContent().stream()
                .map(post -> mapPost(post, userId, likedPostIds.contains(post.getId())))
                .toList();
        return ApiResponse.success("Community feed fetched successfully", new CommunityPostPageResponse(
                content, posts.getNumber(), posts.getSize(), posts.getTotalElements(), posts.getTotalPages(), posts.isFirst(), posts.isLast()));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CommunityPostResponse> getPost(Long postId, Long userId) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        CommunityPost post = findVisiblePost(postId);
        if (post == null) return ApiResponse.error("Community post not found");
        return ApiResponse.success("Community post fetched successfully", mapPost(post, userId, likeRepo.existsByPostIdAndUserId(postId, userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CommunityPostLikeResponse>> getPostLikes(Long postId, Long userId) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        if (findVisiblePost(postId) == null) return ApiResponse.error("Community post not found");
        List<CommunityPostLikeResponse> likes = likeRepo
                .findByPostIdAndUserStatusOrderByCreatedAtDesc(postId, UserStatus.Active)
                .stream()
                .map(like -> new CommunityPostLikeResponse(mapAuthor(like.getUser()), like.getCreatedAt()))
                .toList();
        return ApiResponse.success("Post likes fetched successfully", likes);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CommunityProfileResponse> getCommunityProfile(Long profileUserId, Long userId) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        User profileUser = findActiveUser(profileUserId);
        if (profileUser == null) return ApiResponse.error("Community profile not found");
        Salon salon = profileUser.getSalon() != null ? profileUser.getSalon() : salonRepo.findByOwnerId(profileUserId);
        if (!isActiveSalon(salon)) salon = null;
        return ApiResponse.success("Community profile fetched successfully", new CommunityProfileResponse(
                mapMember(profileUser, userId),
                profileUser.getRole(),
                profileUser.isCareerProfileEnabled(),
                profileUser.isCareerProfileEnabled() ? profileUser.getYearsOfExperience() : null,
                profileUser.isCareerProfileEnabled() ? profileUser.getCareerSkills() : List.of(),
                profileUser.isCareerProfileEnabled() ? profileUser.getPreferredWorkLocations() : List.of(),
                profileUser.isCareerProfileEnabled() ? profileUser.getPortfolioUrl() : null,
                profileUser.isCareerProfileEnabled() ? profileUser.getProfessionalBio() : null,
                salon == null ? null : salon.getId(),
                salon == null ? null : salon.getName(),
                postRepo.countByAuthorIdAndAuthorStatus(profileUserId, UserStatus.Active),
                friendshipRepo.countByRequesterIdAndStatusOrAddresseeIdAndStatus(
                        profileUserId, FriendshipStatus.ACCEPTED, profileUserId, FriendshipStatus.ACCEPTED),
                salonFollowRepo.countByUserId(profileUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CommunityPostPageResponse> getMemberPosts(Long profileUserId, Long userId, Integer page, Integer size) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        if (findActiveUser(profileUserId) == null) return ApiResponse.error("Community profile not found");
        PageRequest pageRequest = pageRequest(page, size);
        if (pageRequest == null) return ApiResponse.error("Page must be non-negative and size must be between 1 and 50");

        Page<CommunityPost> posts = postRepo.findByAuthorIdAndAuthorStatus(profileUserId, UserStatus.Active, pageRequest);
        Set<Long> likedPostIds = likedPostIds(posts.getContent(), userId);
        List<CommunityPostResponse> content = posts.getContent().stream()
                .map(post -> mapPost(post, userId, likedPostIds.contains(post.getId())))
                .toList();
        return ApiResponse.success("Community member posts fetched successfully", new CommunityPostPageResponse(
                content, posts.getNumber(), posts.getSize(), posts.getTotalElements(), posts.getTotalPages(), posts.isFirst(), posts.isLast()));
    }

    @Override
    @Transactional
    public ApiResponse<CommunityPostResponse> createPost(CreatePostRequest request, Long userId) {
        User author = findActiveUser(userId);
        if (author == null) return ApiResponse.error("Active user not found");
        String content = request == null ? null : normalize(request.content());
        String mediaUrl = request == null ? null : normalize(request.mediaUrl());
        String validationError = validatePost(content, mediaUrl);
        if (validationError != null) return ApiResponse.error(validationError);

        Salon salon = null;
        if (request.salonId() != null) {
            salon = findActiveSalon(request.salonId());
            if (salon == null) return ApiResponse.error("Active salon not found");
            if (salon.getOwner() == null || salon.getOwner().getId() != userId) {
                return ApiResponse.error("Only the salon owner can publish as this salon");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        CommunityPost post = new CommunityPost();
        post.setAuthor(author);
        post.setSalon(salon);
        post.setContent(content);
        post.setMediaUrl(mediaUrl);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postRepo.save(post);
        return ApiResponse.success("Community post created successfully", mapPost(post, userId, false));
    }

    @Override
    @Transactional
    public ApiResponse<CommunityPostResponse> updatePost(Long postId, UpdatePostRequest request, Long userId) {
        CommunityPost post = findOwnedPost(postId, userId);
        if (post == null) return ApiResponse.error("Community post owned by you was not found");
        String content = request == null ? null : normalize(request.content());
        String mediaUrl = request == null ? null : normalize(request.mediaUrl());
        String validationError = validatePost(content, mediaUrl);
        if (validationError != null) return ApiResponse.error(validationError);
        post.setContent(content);
        post.setMediaUrl(mediaUrl);
        post.setUpdatedAt(LocalDateTime.now());
        postRepo.save(post);
        return ApiResponse.success("Community post updated successfully", mapPost(post, userId, likeRepo.existsByPostIdAndUserId(postId, userId)));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deletePost(Long postId, Long userId) {
        CommunityPost post = findOwnedPost(postId, userId);
        if (post == null) return ApiResponse.error("Community post owned by you was not found");
        likeRepo.deleteByPostId(postId);
        commentRepo.deleteByPostId(postId);
        postRepo.delete(post);
        return ApiResponse.success("Community post deleted successfully", null);
    }

    @Override
    @Transactional
    public ApiResponse<CommunityPostResponse> likePost(Long postId, Long userId) {
        User user = findActiveUser(userId);
        CommunityPost post = findVisiblePost(postId);
        if (user == null || post == null) return ApiResponse.error("Community post or active user not found");
        if (likeRepo.findByPostIdAndUserId(postId, userId).isEmpty()) {
            CommunityPostLike like = new CommunityPostLike();
            like.setPost(post);
            like.setUser(user);
            like.setCreatedAt(LocalDateTime.now());
            likeRepo.save(like);
        }
        return ApiResponse.success("Community post liked", mapPost(post, userId, true));
    }

    @Override
    @Transactional
    public ApiResponse<CommunityPostResponse> unlikePost(Long postId, Long userId) {
        CommunityPost post = findVisiblePost(postId);
        if (findActiveUser(userId) == null || post == null) return ApiResponse.error("Community post or active user not found");
        likeRepo.findByPostIdAndUserId(postId, userId).ifPresent(likeRepo::delete);
        likeRepo.flush();
        return ApiResponse.success("Community post unliked", mapPost(post, userId, false));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CommunityCommentPageResponse> getComments(Long postId, Long userId, Integer page, Integer size) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        if (findVisiblePost(postId) == null) return ApiResponse.error("Community post not found");
        PageRequest pageRequest = pageRequest(page, size);
        if (pageRequest == null) return ApiResponse.error("Page must be non-negative and size must be between 1 and 50");
        Page<CommunityComment> comments = commentRepo.findByPostIdAndAuthorStatus(postId, UserStatus.Active, pageRequest.withSort(Sort.by("createdAt").ascending()));
        return ApiResponse.success("Post comments fetched successfully", new CommunityCommentPageResponse(
                comments.getContent().stream().map(comment -> mapComment(comment, userId)).toList(),
                comments.getNumber(), comments.getSize(), comments.getTotalElements(), comments.getTotalPages(), comments.isFirst(), comments.isLast()));
    }

    @Override
    @Transactional
    public ApiResponse<CommunityCommentResponse> addComment(Long postId, CreateCommentRequest request, Long userId) {
        User author = findActiveUser(userId);
        CommunityPost post = findVisiblePost(postId);
        if (author == null || post == null) return ApiResponse.error("Community post or active user not found");
        String content = request == null ? null : normalize(request.content());
        if (content == null) return ApiResponse.error("Comment content is required");
        if (content.length() > MAX_COMMENT_LENGTH) return ApiResponse.error("Comment cannot exceed 2000 characters");
        LocalDateTime now = LocalDateTime.now();
        CommunityComment comment = new CommunityComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentRepo.save(comment);
        return ApiResponse.success("Comment added successfully", mapComment(comment, userId));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteComment(Long postId, Long commentId, Long userId) {
        CommunityComment comment = commentId == null || postId == null ? null : commentRepo.findByIdAndPostId(commentId, postId).orElse(null);
        if (comment == null || (comment.getAuthor().getId() != userId && comment.getPost().getAuthor().getId() != userId)) {
            return ApiResponse.error("Comment was not found or you cannot delete it");
        }
        commentRepo.delete(comment);
        return ApiResponse.success("Comment deleted successfully", null);
    }

    @Override
    @Transactional
    public ApiResponse<FriendshipResponse> sendFriendRequest(Long addresseeId, Long userId) {
        User requester = findActiveUser(userId);
        User addressee = findActiveUser(addresseeId);
        if (requester == null || addressee == null) return ApiResponse.error("Active user not found");
        if (userId.equals(addresseeId)) return ApiResponse.error("You cannot add yourself as a friend");
        Long lower = Math.min(userId, addresseeId);
        Long higher = Math.max(userId, addresseeId);
        Friendship existing = friendshipRepo.findByLowerUserIdAndHigherUserId(lower, higher).orElse(null);
        if (existing != null) return ApiResponse.error(existing.getStatus() == FriendshipStatus.ACCEPTED ? "You are already friends" : "A friend request already exists");
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setLowerUserId(lower);
        friendship.setHigherUserId(higher);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());
        friendshipRepo.save(friendship);
        communityNotificationService.friendRequestReceived(friendship);
        return ApiResponse.success("Friend request sent successfully", mapFriendship(friendship, userId));
    }

    @Override
    @Transactional
    public ApiResponse<FriendshipResponse> acceptFriendRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipId == null ? null : friendshipRepo.findById(friendshipId).orElse(null);
        if (friendship == null || friendship.getStatus() != FriendshipStatus.PENDING
                || !Objects.equals(friendship.getAddressee().getId(), userId)) {
            return ApiResponse.error("Incoming friend request not found");
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setRespondedAt(LocalDateTime.now());
        friendshipRepo.save(friendship);
        communityNotificationService.friendRequestAccepted(friendship);
        return ApiResponse.success("Friend request accepted", mapFriendship(friendship, userId));
    }

    @Override
    @Transactional
    public ApiResponse<Void> declineFriendRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipId == null ? null : friendshipRepo.findById(friendshipId).orElse(null);
        if (friendship == null || friendship.getStatus() != FriendshipStatus.PENDING
                || !Objects.equals(friendship.getAddressee().getId(), userId)) {
            return ApiResponse.error("Incoming friend request not found");
        }
        communityNotificationService.friendRequestDeclined(friendship);
        friendshipRepo.delete(friendship);
        return ApiResponse.success("Friend request declined", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeFriendship(Long friendshipId, Long userId) {
        Friendship friendship = friendshipId == null ? null : friendshipRepo.findById(friendshipId).orElse(null);
        if (friendship == null || (friendship.getRequester().getId() != userId && friendship.getAddressee().getId() != userId)) {
            return ApiResponse.error("Friendship or friend request not found");
        }
        friendshipRepo.delete(friendship);
        return ApiResponse.success("Friendship removed successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<FriendshipResponse>> getFriends(Long userId) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        List<FriendshipResponse> friends = friendshipRepo
                .findByRequesterIdAndStatusOrAddresseeIdAndStatusOrderByRespondedAtDesc(userId, FriendshipStatus.ACCEPTED, userId, FriendshipStatus.ACCEPTED)
                .stream().map(friendship -> mapFriendship(friendship, userId)).toList();
        return ApiResponse.success("Friends fetched successfully", friends);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<FriendshipResponse>> getIncomingFriendRequests(Long userId) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        return ApiResponse.success("Incoming friend requests fetched successfully", friendshipRepo
                .findByAddresseeIdAndStatusOrderByCreatedAtDesc(userId, FriendshipStatus.PENDING)
                .stream().map(friendship -> mapFriendship(friendship, userId)).toList());
    }

    @Override
    @Transactional
    public ApiResponse<SalonFollowResponse> followSalon(Long salonId, Long userId) {
        User user = findActiveUser(userId);
        Salon salon = findActiveSalon(salonId);
        if (user == null || salon == null) return ApiResponse.error("Active salon or user not found");
        SalonFollow follow = salonFollowRepo.findByUserIdAndSalonId(userId, salonId).orElse(null);
        if (follow == null) {
            follow = new SalonFollow();
            follow.setUser(user);
            follow.setSalon(salon);
            follow.setCreatedAt(LocalDateTime.now());
            salonFollowRepo.save(follow);
        }
        return ApiResponse.success("Salon followed successfully", mapFollow(follow));
    }

    @Override
    @Transactional
    public ApiResponse<Void> unfollowSalon(Long salonId, Long userId) {
        SalonFollow follow = salonFollowRepo.findByUserIdAndSalonId(userId, salonId).orElse(null);
        if (follow != null) salonFollowRepo.delete(follow);
        return ApiResponse.success("Salon unfollowed successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<SalonFollowResponse>> getFollowedSalons(Long userId) {
        if (findActiveUser(userId) == null) return ApiResponse.error("Active user not found");
        return ApiResponse.success("Followed salons fetched successfully", salonFollowRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(follow -> isActiveSalon(follow.getSalon())).map(this::mapFollow).toList());
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) return null;
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private User findActiveUser(Long id) {
        return id == null ? null : userRepo.findById(id).filter(user -> user.getStatus() == UserStatus.Active).orElse(null);
    }

    private Salon findActiveSalon(Long id) {
        return id == null ? null : salonRepo.findById(id).filter(this::isActiveSalon).orElse(null);
    }

    private boolean isActiveSalon(Salon salon) {
        return salon != null && salon.getStatus() == SalonStatus.Accepted && !salon.isBlockedByAdmin() && !salon.isBlockedByOwner();
    }

    private CommunityPost findVisiblePost(Long id) {
        return id == null ? null : postRepo.findByIdAndAuthorStatus(id, UserStatus.Active).orElse(null);
    }

    private CommunityPost findOwnedPost(Long id, Long userId) {
        CommunityPost post = findVisiblePost(id);
        return post != null && post.getAuthor().getId() == userId ? post : null;
    }

    private String validatePost(String content, String mediaUrl) {
        if (content == null) return "Post content is required";
        if (content.length() > MAX_POST_LENGTH) return "Post content cannot exceed 5000 characters";
        if (mediaUrl != null && !UrlValidationUtils.isValidWebUrl(mediaUrl)) return "Media URL must be a valid HTTP or HTTPS URL";
        return null;
    }

    // Typed "%" or "_" must match literally instead of acting as LIKE wildcards.
    public static String likePrefix(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Set<Long> likedPostIds(List<CommunityPost> posts, Long userId) {
        if (posts.isEmpty()) return Set.of();
        List<Long> ids = posts.stream().map(CommunityPost::getId).toList();
        Set<Long> likedIds = new HashSet<>();
        likeRepo.findByPostIdInAndUserId(ids, userId).forEach(like -> likedIds.add(like.getPost().getId()));
        return likedIds;
    }

    private CommunityPostResponse mapPost(CommunityPost post, Long userId, boolean likedByMe) {
        return new CommunityPostResponse(post.getId(), mapAuthor(post.getAuthor()), mapSalon(post.getSalon()), post.getContent(), post.getMediaUrl(),
                likeRepo.countByPostId(post.getId()), commentRepo.countByPostIdAndAuthorStatus(post.getId(), UserStatus.Active), likedByMe,
                post.getAuthor().getId() == userId, post.getCreatedAt(), post.getUpdatedAt());
    }

    private CommunityCommentResponse mapComment(CommunityComment comment, Long userId) {
        return new CommunityCommentResponse(comment.getId(), comment.getPost().getId(), mapAuthor(comment.getAuthor()), comment.getContent(),
                comment.getAuthor().getId() == userId, comment.getCreatedAt(), comment.getUpdatedAt());
    }

    private FriendshipResponse mapFriendship(Friendship friendship, Long userId) {
        boolean incoming = friendship.getAddressee().getId() == userId;
        User other = incoming ? friendship.getRequester() : friendship.getAddressee();
        return new FriendshipResponse(friendship.getId(), mapAuthor(other), friendship.getStatus(), incoming, friendship.getCreatedAt(), friendship.getRespondedAt());
    }

    private SalonFollowResponse mapFollow(SalonFollow follow) {
        return new SalonFollowResponse(mapSalon(follow.getSalon()), follow.getCreatedAt());
    }

    private CommunityAuthorResponse mapAuthor(User user) {
        String displayName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return new CommunityAuthorResponse(user.getId(), user.getUserName(), displayName, user.getProfilePictureUrl());
    }

    private CommunitySalonResponse mapSalon(Salon salon) {
        return salon == null ? null : new CommunitySalonResponse(salon.getId(), salon.getName(), salon.getProfilePictureUrl());
    }

    private CommunityMemberResponse mapMember(User member, Long userId) {
        Friendship friendship = friendshipRepo
                .findByLowerUserIdAndHigherUserId(Math.min(member.getId(), userId), Math.max(member.getId(), userId))
                .orElse(null);
        if (friendship == null) return new CommunityMemberResponse(mapAuthor(member), null, "NONE", false);
        boolean incoming = friendship.getAddressee().getId() == userId;
        return new CommunityMemberResponse(mapAuthor(member), friendship.getId(), friendship.getStatus().name(), incoming);
    }
}
