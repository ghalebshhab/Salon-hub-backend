package com.salon.backend.Controllers.Community;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Community.*;
import com.salon.backend.Services.Auth.Security.AuthenticatedUser;
import com.salon.backend.Services.Community.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/people")
    public ApiResponse<List<CommunityMemberResponse>> searchMembers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.searchMembers(currentUser.getId(), search, size);
    }

    @GetMapping("/feed")
    public ApiResponse<CommunityPostPageResponse> getFeed(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getFeed(currentUser.getId(), page, size);
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> getPost(@PathVariable Long postId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getPost(postId, currentUser.getId());
    }

    @GetMapping("/posts/{postId}/likes")
    public ApiResponse<List<CommunityPostLikeResponse>> getPostLikes(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getPostLikes(postId, currentUser.getId());
    }

    @GetMapping("/people/{userId}")
    public ApiResponse<CommunityProfileResponse> getCommunityProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getCommunityProfile(userId, currentUser.getId());
    }

    @GetMapping("/people/{userId}/posts")
    public ApiResponse<CommunityPostPageResponse> getMemberPosts(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getMemberPosts(userId, currentUser.getId(), page, size);
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityPostResponse> createPost(@RequestBody CreatePostRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.createPost(request, currentUser.getId());
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<CommunityPostResponse> updatePost(@PathVariable Long postId, @RequestBody UpdatePostRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.updatePost(postId, request, currentUser.getId());
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.deletePost(postId, currentUser.getId());
    }

    @PutMapping("/posts/{postId}/like")
    public ApiResponse<CommunityPostResponse> likePost(@PathVariable Long postId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.likePost(postId, currentUser.getId());
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<CommunityPostResponse> unlikePost(@PathVariable Long postId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.unlikePost(postId, currentUser.getId());
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentPageResponse> getComments(
            @PathVariable Long postId, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getComments(postId, currentUser.getId(), page, size);
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentResponse> addComment(
            @PathVariable Long postId, @RequestBody CreateCommentRequest request, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.addComment(postId, request, currentUser.getId());
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId, @PathVariable Long commentId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.deleteComment(postId, commentId, currentUser.getId());
    }

    @PostMapping("/friends/requests/{userId}")
    public ApiResponse<FriendshipResponse> sendFriendRequest(@PathVariable Long userId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.sendFriendRequest(userId, currentUser.getId());
    }

    @PutMapping("/friends/requests/{friendshipId}/accept")
    public ApiResponse<FriendshipResponse> acceptFriendRequest(@PathVariable Long friendshipId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.acceptFriendRequest(friendshipId, currentUser.getId());
    }

    @DeleteMapping("/friends/requests/{friendshipId}")
    public ApiResponse<Void> declineFriendRequest(@PathVariable Long friendshipId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.declineFriendRequest(friendshipId, currentUser.getId());
    }

    @DeleteMapping("/friends/{friendshipId}")
    public ApiResponse<Void> removeFriendship(@PathVariable Long friendshipId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.removeFriendship(friendshipId, currentUser.getId());
    }

    @GetMapping("/friends")
    public ApiResponse<List<FriendshipResponse>> getFriends(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getFriends(currentUser.getId());
    }

    @GetMapping("/friends/requests")
    public ApiResponse<List<FriendshipResponse>> getIncomingFriendRequests(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getIncomingFriendRequests(currentUser.getId());
    }

    @PutMapping("/salons/{salonId}/follow")
    public ApiResponse<SalonFollowResponse> followSalon(@PathVariable Long salonId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.followSalon(salonId, currentUser.getId());
    }

    @DeleteMapping("/salons/{salonId}/follow")
    public ApiResponse<Void> unfollowSalon(@PathVariable Long salonId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.unfollowSalon(salonId, currentUser.getId());
    }

    @GetMapping("/salons/following")
    public ApiResponse<List<SalonFollowResponse>> getFollowedSalons(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return communityService.getFollowedSalons(currentUser.getId());
    }
}
