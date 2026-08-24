# Approved Visual Product Implementation Matrix

This matrix audits the PDF plus all eight approved raster references. “Implemented” means a real production path backed by existing or newly added state/data behavior. “Not yet implemented” is used whenever a visible target capability remains only partial; partial work is described so it cannot be mistaken for completion.

| Visual reference | Screen/capability | Previous state | Implementation performed | Backend/data work if any | Final status |
|---|---|---|---|---|---|
| 1 Master + 2 Canonical Home | Shared light design system | Mixed legacy surfaces | Nova palette, type, spacing, cards, buttons, states, icons, five-destination navigation and shared shell were converged; canonical Home remains spacing authority | None | Implemented to target design |
| 1 + 2 | Home composition | Feed existed without the approved hierarchy | Canonical greeting/header, Tonight hero, feed card, Pulse/Orbit/Rooms/Memories rails and bottom navigation were implemented | Existing feed, Tonight, Pulse, Orbit, Rooms and Memories APIs | Implemented to target design |
| 5 Core Social | Post detail | Functional legacy detail | Target post hierarchy, author/media/actions/comment preview and real navigation retained | Existing post API | Implemented to target design |
| 5 Core Social | Comments and replies | Real comments/replies; no per-thread reactions | Added target thread rows, reply hierarchy, heart/count controls and optimistic state | Added persisted post-comment and comment-reply likes plus endpoints | Implemented to target design |
| 5 Core Social | Story viewer/composer | Real stories existed | Immersive viewer, progress, reply/share composer and story creation retained/converged | Existing Story persistence/audience API; default audience now honors privacy setting | Implemented to target design |
| 5 Core Social | Reels / short video | Real feed, recording/upload, reactions and sharing existed | Dark immersive presentation/navigation retained and aligned to shared design | Existing Reel APIs/watch signals | Implemented to target design |
| 1 + 5 | Own profile | Functional legacy profile | Orbit-ring identity, counts, bio/location/interests, content grid, edit/settings/navigation converged | Expanded user metadata | Implemented to target design |
| 3 Discovery | People search | Search existed | Approved search layout plus persisted recent-person history | Local recent-search persistence | Implemented to target design |
| 3 Discovery | Discover filters | Single unfiltered discovery list | Added People/Nearby/Interests/Verified/New controls with server-backed filtering | Added safe discovery query filters; verification remains server/admin controlled | Implemented to target design |
| 3 Discovery | Follow requests Received/Sent | Incoming requests existed in Privacy/Activity; sent list absent | Added Received/Sent presentation and real pending-sent data | Added authenticated sent-follow-request endpoint | Implemented to target design |
| 3 Discovery | Followers / Following | Real paginated lists existed | Connections presentation and messaging actions retained/converged | Existing social graph APIs | Implemented to target design |
| 3 Discovery | Other-user profile | Real profile/follow/message/safety existed | Added approved metadata, orbit identity, audio/video call actions and preserved share/block/report | Expanded public profile payload; reuses real conversation/call APIs | Implemented to target design |
| 3 Discovery | Edit profile | Name/avatar only | Added handle, bio, location, link, interests, show-orbit and profile-theme controls | Added persisted profile metadata and validation | Implemented to target design |
| 1 + 6 Live/Orbit | Orbit hub | Real relationship/presence list existed | Approved orbit rings, presence states, filters, profile/message entry points and Tonight/Rooms composition retained | Existing presence WebSocket and Orbit APIs | Implemented to target design |
| 6 Live/Orbit | Orbit detail/activity composition | Profile, messaging, calls and content exist as separate real paths | Message/audio/video entry points and profile metadata implemented | Reuses messaging/call/profile APIs | Not yet implemented — the single approved dark Orbit Activity composition with live joins is not fully assembled |
| 1 + 6 | Pulse feed/categories | Real expiring Pulse feed existed without categories | Added All/Live/Music/Talks/Vibes filtering and category-aware creation/replies | Added persisted Pulse category and feed filter | Implemented to target design |
| 6 Live/Orbit | Pulse immersive viewer/live reactions | Viewer and threaded Pulse replies exist | Category-aware reply chain and immersive media retained | Existing Pulse chain API | Not yet implemented — approved viewer heart/reaction stream and live viewer-count behavior are absent |
| 6 Live/Orbit | Pulse composer | Real text/photo/video Pulse composer existed | Added approved category selection while preserving audience/media validation | Category added to create API | Implemented to target design |
| 1 + 6 | Tonight full | Real Tonight snapshot and room joins existed | Dark orbit hero, live people and Rooms composition/navigation converged | Existing Tonight/Rooms APIs | Implemented to target design |
| 7 Rooms/Messaging | Inbox | Real inbox/search/paging/unread existed | Added approved All/Unread/Mentions controls using full server filters | Added authenticated inbox filters including current-user mentions | Implemented to target design |
| 7 Rooms/Messaging | Direct chat | Full real-time chat existed | Target chat chrome, media/voice/replies/reactions/read state/calls retained and converged | Existing REST/WebSocket delivery and read-receipt preference | Implemented to target design |
| 7 Rooms/Messaging | Group chat | Group chat/admin/media/calls existed | Target group identity, live entry, message/reaction/composer behavior retained | Existing group membership/realtime APIs | Implemented to target design |
| 1 + 7 | My Rooms list | Group-backed Rooms list existed | Approved Room cards, unread state and detail navigation retained | Existing group/Room APIs | Implemented to target design |
| 7 Rooms/Messaging | Room Discover/Following/public Join | No public-room discovery or join/follow domain | No fake public rooms or join buttons added | Requires public visibility, discovery ranking, membership policy and join/follow API work | Not yet implemented |
| 7 Rooms/Messaging | Room detail/private shared space | Real group-backed Room detail/items/members existed | Approved sections, members, description, notes/media/music/plans/saved, pins and live-chat entry retained/converged | Existing Room item APIs | Implemented to target design |
| 7 Rooms/Messaging | Scheduled plans/reminders | Plans existed without date picker or reminders | Added real date/time selection, persisted reminder toggle and local 15-minute Android alarm notification | Added RoomReminder model/endpoints | Implemented to target design |
| 7 Rooms/Messaging | Active audio/video call | Production WebRTC call flow existed | Dark call surface, orbit participants and real call controls retained | Existing call sessions/signaling/WebRTC/TURN configuration | Implemented to target design |
| 4 Account | Notification preferences | Activity feed existed; granular settings absent | Added approved eight-toggle preference screen with real loading/error/session behavior | Added persisted preferences and push-delivery enforcement where corresponding event paths exist | Implemented to target design |
| 1 + 4 | Settings | Narrow legacy settings | Added Account, Privacy, Notifications, Appearance status, Data & Storage, Language status, Help, Tonight, About, blocked accounts, policy/deletion and logout; only actionable rows are real | Platform storage intent; real support contact; existing account APIs | Implemented to target design |
| 4 Account | Privacy | Private account/requests/Close Friends existed | Added activity status, read receipts, story audience and Received/Sent requests | Added persisted privacy fields; enforced presence/read-receipt/story defaults | Implemented to target design |
| 4 Account | Blocked accounts | Real blocking existed | Approved list/unblock flow retained/converged | Existing trust/safety API | Implemented to target design |
| 4 Account | Password/session protection | Change password and revoke-other-sessions existed | Target security grouping retained; password recovery/change/revoke/delete remain real | Existing password-bound JWT security | Implemented to target design |
| 4 Account | App lock | Absent | Added opt-in device-credential app lock, enrollment confirmation and foreground enforcement | Local protected preference + Android Keyguard | Implemented to target design |
| 4 Account | Two-factor authentication | Absent | Not represented as a working toggle because the approved image does not choose authenticator TOTP vs email/SMS | Requires an explicit factor/recovery policy and corresponding enrollment/login protocol | Not yet implemented — unresolved product/security decision |
| 4 Account | Login activity/session list | Only revoke-all-other-sessions existed | Existing real revocation retained | Current JWT model does not persist per-device session records | Not yet implemented |
| 4 Account | Login | Real email/password login/recovery existed | Approved wordmark/form hierarchy and recovery entry converged; no fake provider buttons | Existing secure token/session API | Implemented to target design |
| 4 Account | Google/Apple/social login | Absent; provider configuration unavailable | No fake OAuth controls added | Requires provider client IDs/secrets, redirect configuration and account-linking policy | Not yet implemented — external configuration blocker |
| 4 Account | Register/onboarding | Registration plus separate setup existed | Approved single real full-name/username/email/password creation flow implemented | Existing registration API, expanded profile supports later edit | Implemented to target design |
| 8 Memories | Create hub | Separate creators/rails existed | Approved Post/Story/Pulse/Room/Memory entry composition implemented; every action opens a real creator or real content section | Existing feature repositories | Implemented to target design |
| 8 Memories | Weekly Memories home/detail/recap | Real generated weekly recap existed | Paper-toned weekly recap, date navigation, stats, highlights, people and sharing retained/converged | Existing server-generated weekly Memory API | Implemented to target design |
| 8 Memories | New Memory and Recent Drafts | No user-authored Memory draft domain | Existing generated recap/film paths were not mislabeled as drafts | Requires draft model, media selection/editing, autosave and draft CRUD | Not yet implemented |
| 8 Memories | Memory film builder/storyboard | Real server plan and local Media3 exporter existed | Storyboard scenes, preview, cancellation, export and external sharing retained/converged | Existing film-plan API; local Media3 MP4 export | Implemented to target design |
| 8 Memories | Background rendering/resume | Export tied to active UI scope | Progress and cancellation exist while active | Requires durable foreground WorkManager job/output persistence | Not yet implemented |
| 8 Memories | Preview/share/save | Real rendered-file preview and Android share existed | Approved ready state, preview and system share implemented | Local exported MP4/FileProvider | Implemented to target design |

## Completion boundary

The approved visual/product implementation is **not complete yet**. The remaining intentional capabilities are explicitly limited to:

- assembled Orbit Activity detail/live-join composition;
- Pulse live reactions/viewer-count stream;
- public/discoverable/followable Rooms and join policy;
- two-factor authentication factor/recovery policy and implementation;
- persisted per-device login activity;
- OAuth provider setup and account linking;
- user-authored Memory drafts;
- durable background Memory film rendering.

No row above should be treated as complete merely because an adjacent existing feature works.
