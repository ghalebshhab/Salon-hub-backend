package com.salon.backend.Services.Notification;

import com.salon.backend.Entities.community.Friendship;

public interface CommunityNotificationService {
    void friendRequestReceived(Friendship friendship);
    void friendRequestAccepted(Friendship friendship);
    void friendRequestDeclined(Friendship friendship);
}
