# REST, WebSocket, and Android entry-contract inventory

Snapshot: `dcbd534f6ba6656832d08b55d94097a302c8e99b`.

This is a protected-contract inventory. A structural PR may move the owning
module but must keep paths, methods, names, payload semantics, authentication,
and status behavior unless a separate contract change is approved.

## Public/project routes

| Path | Methods | Name/owner |
|---|---|---|
| `/admin/` | Django admin | `admin.site.urls` |
| `/privacy/` | GET | `privacy-policy`, template view |
| `/account-deletion/` | GET | `account-deletion`, template view |
| `/child-safety/` | GET | `child-safety`, template view |
| `/api/v1/health/` | GET | `health`, `nova_backend.urls.HealthView` |

`/api/v1/` includes `accounts.reels_urls` followed by `accounts.urls`.

## Auth, identity, privacy, and social REST routes

| Path | Methods | URL name | Current owner |
|---|---|---|---|
| `/api/v1/auth/register/` | POST | `register` | `account_security.SecureRegisterView` |
| `/api/v1/auth/login/` | POST | `login` | `account_security.SecureTokenObtainPairView` |
| `/api/v1/auth/refresh/` | POST | `token-refresh` | `account_security.SecureTokenRefreshView` |
| `/api/v1/auth/password/reset/request/` | POST | `password-reset-request` | `account_security.PasswordResetRequestView` |
| `/api/v1/auth/password/reset/confirm/` | POST | `password-reset-confirm` | `account_security.PasswordResetConfirmView` |
| `/api/v1/auth/password/change/` | POST | `password-change` | `account_security.ChangePasswordView` |
| `/api/v1/auth/sessions/revoke-others/` | POST | `revoke-other-sessions` | `account_security.RevokeOtherSessionsView` |
| `/api/v1/auth/account/delete/` | POST | `account-delete` | `trust_safety.DeleteAccountView` |
| `/api/v1/auth/blocks/` | GET | `blocked-users` | `trust_safety.BlockedUsersView` |
| `/api/v1/me/` | GET, PUT, PATCH | `me` | `views.MeView` |
| `/api/v1/privacy/` | GET, POST | `account-privacy` | `privacy_views.AccountPrivacyView` |
| `/api/v1/follow-requests/` | GET | `follow-requests` | `privacy_views.FollowRequestsView` |
| `/api/v1/follow-requests/<int:request_id>/accept/` | POST | `follow-request-accept` | `privacy_views.FollowRequestAcceptView` |
| `/api/v1/follow-requests/<int:request_id>/decline/` | POST | `follow-request-decline` | `privacy_views.FollowRequestDeclineView` |
| `/api/v1/close-friends/` | GET, POST | `close-friends` | `privacy_views.CloseFriendsView` |
| `/api/v1/close-friends/<str:username>/` | DELETE | `close-friend-detail` | `privacy_views.CloseFriendDetailView` |
| `/api/v1/people/` | GET | `people` | `social_paging.PaginatedPeopleView` |
| `/api/v1/people/<str:username>/` | GET | `person-detail` | `views.PersonView` |
| `/api/v1/people/<str:username>/posts/` | GET | `person-posts` | `social_paging.PaginatedPersonPostsView` |
| `/api/v1/people/<str:username>/reposts/` | GET | `person-reposts` | `social_paging.PaginatedPersonRepostsView` |
| `/api/v1/people/<str:username>/followers/` | GET | `person-followers` | `social_paging.FollowersView` |
| `/api/v1/people/<str:username>/following/` | GET | `person-following-list` | `social_paging.FollowingView` |
| `/api/v1/people/<str:username>/follow/` | POST, DELETE | `person-follow` | `views.FollowView` |
| `/api/v1/people/<str:username>/block/` | POST, DELETE | `person-block` | `trust_safety.UserBlockView` |
| `/api/v1/people/<str:username>/report/` | POST | `person-report` | `trust_safety.UserReportView` |

## Posts, Stories, Reels, sharing, and notifications REST routes

| Path | Methods | URL name | Current owner |
|---|---|---|---|
| `/api/v1/stories/` | GET, POST | `stories` | `stories.StoryFeedView` |
| `/api/v1/stories/<int:story_id>/` | DELETE | `story-detail` | `stories.StoryDetailView` |
| `/api/v1/stories/<int:story_id>/view/` | POST | `story-view` | `stories.StoryViewedView` |
| `/api/v1/stories/<int:story_id>/viewers/` | GET | `story-viewers` | `stories.StoryViewersView` |
| `/api/v1/stories/<int:story_id>/reaction/` | POST, DELETE | `story-reaction` | `stories.StoryReactionView` |
| `/api/v1/stories/<int:story_id>/reply/` | POST | `story-reply` | `stories.StoryReplyView` |
| `/api/v1/posts/` | POST | `posts` | `views.PostsView` |
| `/api/v1/posts/<int:post_id>/` | GET, DELETE | `post-detail` | `views.PostDetailView` |
| `/api/v1/posts/<int:post_id>/like/` | POST, DELETE | `post-like` | `views.PostLikeView` |
| `/api/v1/posts/<int:post_id>/repost/` | GET, POST, DELETE | `post-repost` | `sharing_views.PostRepostView` |
| `/api/v1/posts/<int:post_id>/comments/` | GET, POST | `post-comments` | `comment_threads.ThreadPostCommentsView` |
| `/api/v1/comments/<int:comment_id>/` | DELETE | `comment-detail` | `comment_threads.ThreadCommentDetailView` |
| `/api/v1/comment-replies/<int:reply_id>/` | DELETE | `comment-reply-detail` | `comment_threads.PostCommentReplyDetailView` |
| `/api/v1/feed/` | GET | `feed` | `sharing_views.SharingFeedView` |
| `/api/v1/shares/messages/` | POST | `message-share` | `sharing_views.MessageShareView` |
| `/api/v1/notifications/` | GET | `notifications` | `views.NotificationsView` |
| `/api/v1/notifications/read/` | POST | `notifications-read` | `views.NotificationsReadView` |
| `/api/v1/push/devices/` | POST, DELETE | `push-devices` | `views.DevicePushTokenView` |
| `/api/v1/reels/` | GET, POST | `reels` | `reels.ReelFeedView` |
| `/api/v1/reels/profile/<str:username>/` | GET | `profile-reels` | `profile_reels.ProfileReelsView` |
| `/api/v1/reels/<int:reel_id>/` | GET, DELETE | `reel-detail` | `reels.ReelDetailView` |
| `/api/v1/reels/<int:reel_id>/watch/` | POST | `reel-watch` | `reels.ReelWatchView` |
| `/api/v1/reels/<int:reel_id>/like/` | POST, DELETE | `reel-like` | `reels.ReelLikeView` |
| `/api/v1/reels/<int:reel_id>/repost/` | POST, DELETE | `reel-repost` | `reels.ReelRepostView` |
| `/api/v1/reels/<int:reel_id>/comments/` | GET, POST | `reel-comments` | `comment_threads.ThreadReelCommentsView` |
| `/api/v1/reel-comments/<int:comment_id>/` | DELETE | `reel-comment-detail` | `comment_threads.ThreadReelCommentDetailView` |
| `/api/v1/reel-comment-replies/<int:reply_id>/` | DELETE | `reel-comment-reply-detail` | `comment_threads.ReelCommentReplyDetailView` |

## Calls and messaging REST routes

| Path | Methods | URL name | Current owner |
|---|---|---|---|
| `/api/v1/calls/` | POST | `call-create` | `call_reliability_view.ReliableCallSessionCreateView` |
| `/api/v1/calls/ice/` | GET | `call-ice-config` | `calls.CallIceConfigView` |
| `/api/v1/calls/<uuid:call_id>/` | GET | `call-detail` | `calls.CallSessionDetailView` |
| `/api/v1/calls/<uuid:call_id>/action/` | POST | `call-action` | `calls.CallSessionActionView` |
| `/api/v1/conversations/` | GET, POST | `conversations` | GET `messaging_paging.PaginatedConversationsView`; POST behavior is part of the existing conversation-open contract and must be verified before URL reorganization |
| `/api/v1/conversations/groups/` | POST | `group-conversation-create` | `group_messaging.GroupConversationCreateView` |
| `/api/v1/conversations/<int:conversation_id>/group/` | GET, PATCH, DELETE | `group-conversation-detail` | `group_messaging.GroupConversationDetailView` |
| `/api/v1/conversations/<int:conversation_id>/group/manage/` | GET, POST | `group-management-detail` | `group_management.GroupManagementDetailView` |
| `/api/v1/conversations/<int:conversation_id>/group/members/` | POST | `group-members` | `group_messaging.GroupMembersView` |
| `/api/v1/conversations/<int:conversation_id>/group/members/<str:username>/` | DELETE | `group-member-detail` | `group_messaging.GroupMemberDetailView` |
| `/api/v1/conversations/<int:conversation_id>/group/members/<str:username>/role/` | POST | `group-member-role` | `group_management.GroupMemberRoleView` |
| `/api/v1/conversations/<int:conversation_id>/messages/` | GET, POST | `conversation-messages` | `messaging_views.ConversationMessagesView` |
| `/api/v1/conversations/<int:conversation_id>/messages/search/` | GET | `conversation-message-search` | `messaging_v9_views.ConversationMessageSearchView` |
| `/api/v1/conversations/<int:conversation_id>/messages/context/` | GET | `conversation-message-context` | `messaging_v9_views.ConversationMessageContextView` |
| `/api/v1/conversations/<int:conversation_id>/media/` | GET | `conversation-media` | `messaging_v9_views.ConversationMediaView` |
| `/api/v1/conversations/<int:conversation_id>/preferences/` | GET, POST | `conversation-preferences` | `messaging_v9_views.ConversationPreferenceView` |
| `/api/v1/conversations/<int:conversation_id>/read/` | POST | `conversation-read` | `messaging_views.ConversationReadView` |
| `/api/v1/messages/<int:message_id>/` | PATCH, POST, DELETE | `message-detail` | `messaging_mutation_view.MessageMutationView` + inherited delete |
| `/api/v1/messages/<int:message_id>/reaction/` | POST, DELETE | `message-reaction` | `messaging_views.MessageReactionView` |

`conversations/` is currently wired to `PaginatedConversationsView`; existing
tests also exercise POST on that named URL. Preserve the observed method
contract even if static class inspection exposes only the GET override.

## WebSocket routes

All paths are mounted by `nova_backend.asgi` through
`SecureJwtAuthMiddleware(URLRouter(accounts.routing.websocket_urlpatterns))`.

| Path expression | Current consumer | Protected purpose |
|---|---|---|
| `^ws/presence/$` | `realtime.PresenceConsumer` | authenticated presence and last-seen updates |
| `^ws/conversations/(?P<conversation_id>\d+)/$` | `realtime.ConversationConsumer` | message/reaction/edit/delete/read/typing realtime contract |
| `^ws/calls/(?P<call_id>[0-9a-fA-F-]+)/$` | `call_reliability_realtime.ReliableCallConsumer` | call signaling/reliability and negotiation IDs |

## Android notification and intent contracts

FCM data is copied without renaming from `NovaMessagingService` to the launch
intent. Protected push keys observed by routing code include:

- `kind`
- `conversation_id`
- `conversation_kind`
- `group_title`
- `actor_username`
- `actor_name`
- `actor_avatar_url`
- `post_id`
- `reel_id`
- `reel_author_username`

`MainActivity` special-cases `kind=message` and Reel activity kinds
`reel_like`, `reel_comment`, `reel_repost`, and `reel_reply`. Other non-empty
targets flow through `NovaPushOpenSignal`.

Direct conversation Activity extras:

| Constant | String value |
|---|---|
| `EXTRA_CONVERSATION_ID` | `nova_conversation_id` |
| `EXTRA_USERNAME` | `nova_conversation_username` |
| `EXTRA_DISPLAY_NAME` | `nova_conversation_display_name` |
| `EXTRA_AVATAR_URL` | `nova_conversation_avatar_url` |
| `EXTRA_KIND` | `nova_conversation_kind` |
| `EXTRA_MEMBERS_COUNT` | `nova_conversation_members_count` |
| `EXTRA_CURRENT_USER_ROLE` | `nova_conversation_current_user_role` |

Profile-Reel Activity extras:

| Constant | String value |
|---|---|
| `EXTRA_PROFILE_USERNAME` | `profile_username` |
| `EXTRA_INITIAL_REEL_ID` | `initial_reel_id` |

These keys and default semantics are compatibility contracts for notifications,
special Activities, and internal navigators.
