package com.salon.backend.Services.Community;

import com.salon.backend.DTOs.ApiResponse;
import com.salon.backend.DTOs.Community.*;

import java.util.List;

public interface CommunityService {
    ApiResponse<List<CommunityMemberResponse>> searchMembers(Long userId, String search, Integer size);
    ApiResponse<CommunityPostPageResponse> getFeed(Long userId, Integer page, Integer size);
    ApiResponse<CommunityPostResponse> getPost(Long postId, Long userId);
    ApiResponse<List<CommunityPostLikeResponse>> getPostLikes(Long postId, Long userId);
    ApiResponse<CommunityProfileResponse> getCommunityProfile(Long profileUserId, Long userId);
    ApiResponse<CommunityPostResponse> createPost(CreatePostRequest request, Long userId);
    ApiResponse<CommunityPostResponse> updatePost(Long postId, UpdatePostRequest request, Long userId);
    ApiResponse<Void> deletePost(Long postId, Long userId);
    ApiResponse<CommunityPostResponse> likePost(Long postId, Long userId);
    ApiResponse<CommunityPostResponse> unlikePost(Long postId, Long userId);
    ApiResponse<CommunityCommentPageResponse> getComments(Long postId, Long userId, Integer page, Integer size);
    ApiResponse<CommunityCommentResponse> addComment(Long postId, CreateCommentRequest request, Long userId);
    ApiResponse<Void> deleteComment(Long postId, Long commentId, Long userId);
    ApiResponse<FriendshipResponse> sendFriendRequest(Long addresseeId, Long userId);
    ApiResponse<FriendshipResponse> acceptFriendRequest(Long friendshipId, Long userId);
    ApiResponse<Void> declineFriendRequest(Long friendshipId, Long userId);
    ApiResponse<Void> removeFriendship(Long friendshipId, Long userId);
    ApiResponse<List<FriendshipResponse>> getFriends(Long userId);
    ApiResponse<List<FriendshipResponse>> getIncomingFriendRequests(Long userId);
    ApiResponse<SalonFollowResponse> followSalon(Long salonId, Long userId);
    ApiResponse<Void> unfollowSalon(Long salonId, Long userId);
    ApiResponse<List<SalonFollowResponse>> getFollowedSalons(Long userId);
}
