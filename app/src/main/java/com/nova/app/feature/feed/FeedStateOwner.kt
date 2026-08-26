package com.nova.app.feature.feed

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.feed.data.FeedRepository
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.data.PostRepostRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


data class FeedUiState(
    val userId: Long? = null,
    val posts: List<NovaPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val errorMessage: String? = null,
    val deletingPostId: Long? = null,
    val likingPostIds: Set<Long> = emptySet(),
    val repostingPostIds: Set<Long> = emptySet(),
    val isUploadingPost: Boolean = false,
    val postErrorMessage: String? = null,
    val actionErrorPostId: Long? = null,
    val actionErrorMessage: String? = null,
    val contentVersion: Int = 0,
    val sessionExpiryVersion: Int = 0,
    val profileRefreshVersion: Int = 0,
    val postCreatedVersion: Int = 0,
) {
    val hasMore: Boolean
        get() = nextCursor != null
}


/** Lifecycle-scoped owner for feed paging and mutations shared across social-content routes. */
class FeedStateOwner(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val repostRepository: PostRepostRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(FeedUiState())
        private set

    private var accountGeneration = 0
    private var actionRevision = 0
    private val likeRevisions = mutableMapOf<Long, Int>()
    private val repostRevisions = mutableMapOf<Long, Int>()
    private var feedMutationRevision = 0
    private val postStateRevisions = mutableMapOf<Long, Int>()
    private val createdPostRevisions = mutableMapOf<Long, Int>()
    private val removedPostRevisions = mutableMapOf<Long, Int>()
    private val removedAuthorRevisions = mutableMapOf<Long, Int>()
    private var firstPageJob: Job? = null

    fun reset() {
        firstPageJob?.cancel()
        firstPageJob = null
        accountGeneration += 1
        clearMutationTracking()
        state = FeedUiState(
            contentVersion = state.contentVersion + 1,
            sessionExpiryVersion = state.sessionExpiryVersion,
            profileRefreshVersion = state.profileRefreshVersion,
            postCreatedVersion = state.postCreatedVersion,
        )
    }

    fun enter(userId: Long) {
        if (userId <= 0L) {
            reset()
            return
        }
        if (state.userId == userId) return

        firstPageJob?.cancel()
        accountGeneration += 1
        clearMutationTracking()
        val generation = accountGeneration
        val refreshActionRevision = actionRevision
        val refreshFeedMutationRevision = feedMutationRevision
        val cached = feedRepository.cachedFeed(userId)
        state = FeedUiState(
            userId = userId,
            posts = cached?.posts.orEmpty(),
            nextCursor = cached?.nextCursor,
            isLoading = true,
            contentVersion = state.contentVersion + 1,
            sessionExpiryVersion = state.sessionExpiryVersion,
            profileRefreshVersion = state.profileRefreshVersion,
            postCreatedVersion = state.postCreatedVersion,
        )
        firstPageJob = scope.launch {
            refreshFirstPage(
                expectedUserId = userId,
                generation = generation,
                refreshActionRevision = refreshActionRevision,
                refreshFeedMutationRevision = refreshFeedMutationRevision,
            )
        }
    }

    fun clearPostError() {
        state = state.copy(postErrorMessage = null)
    }

    fun loadFeed() {
        if (state.isLoading || state.isLoadingMore) return
        firstPageJob = scope.launch { loadFeedNow() }
    }

    internal suspend fun loadFeedNow() {
        if (state.isLoading || state.isLoadingMore) return
        val expectedUserId = state.userId
        val generation = accountGeneration
        val refreshActionRevision = actionRevision
        val refreshFeedMutationRevision = feedMutationRevision
        state = state.copy(
            isLoading = true,
            errorMessage = null,
            actionErrorPostId = null,
            actionErrorMessage = null,
        )
        refreshFirstPage(
            expectedUserId = expectedUserId,
            generation = generation,
            refreshActionRevision = refreshActionRevision,
            refreshFeedMutationRevision = refreshFeedMutationRevision,
        )
    }

    private suspend fun refreshFirstPage(
        expectedUserId: Long?,
        generation: Int,
        refreshActionRevision: Int = actionRevision,
        refreshFeedMutationRevision: Int = feedMutationRevision,
    ) {
        when (val result = feedRepository.feed()) {
            is ApiResult.Success -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                state = state.copy(
                    posts = reconcileRefreshPosts(
                        serverPosts = result.value.posts,
                        refreshActionRevision = refreshActionRevision,
                        refreshFeedMutationRevision = refreshFeedMutationRevision,
                    ),
                    nextCursor = result.value.nextCursor,
                    isLoading = false,
                )
            }

            is ApiResult.Failure -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                state = if (result.statusCode == 401) {
                    state.copy(
                        isLoading = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun loadMore() {
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore) return
        scope.launch { loadMoreNow(cursor) }
    }

    internal suspend fun loadMoreNow(cursor: String) {
        if (state.isLoading || state.isLoadingMore) return
        val expectedUserId = state.userId
        val generation = accountGeneration
        state = state.copy(isLoadingMore = true, errorMessage = null)
        when (val result = feedRepository.feed(cursor)) {
            is ApiResult.Success -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                state = state.copy(
                    posts = mergeFeedPage(state.posts, result.value.posts),
                    nextCursor = result.value.nextCursor,
                    isLoadingMore = false,
                )
            }

            is ApiResult.Failure -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                state = if (result.statusCode == 401) {
                    state.copy(
                        isLoadingMore = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isLoadingMore = false, errorMessage = result.message)
                }
            }
        }
    }

    fun deletePost(post: NovaPost) {
        if (state.deletingPostId != null || !post.isMine) return
        scope.launch { deletePostNow(post) }
    }

    internal suspend fun deletePostNow(post: NovaPost) {
        if (state.deletingPostId != null || !post.isMine) return
        val expectedUserId = state.userId
        val generation = accountGeneration
        state = state.copy(deletingPostId = post.id, errorMessage = null)
        when (val result = postRepository.deletePost(post.id)) {
            is ApiResult.Success -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                markPostRemoved(post.id)
                state = state.copy(
                    posts = state.posts.filterNot { it.id == post.id },
                    deletingPostId = null,
                    contentVersion = state.contentVersion + 1,
                    profileRefreshVersion = state.profileRefreshVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                state = if (result.statusCode == 401) {
                    state.copy(
                        deletingPostId = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(deletingPostId = null, errorMessage = result.message)
                }
            }
        }
    }

    fun toggleLike(post: NovaPost) {
        if (post.id in state.likingPostIds) return
        scope.launch { toggleLikeNow(post) }
    }

    internal suspend fun toggleLikeNow(post: NovaPost) {
        if (post.id in state.likingPostIds) return
        val expectedUserId = state.userId
        val generation = accountGeneration
        val current = state.posts.firstOrNull { it.id == post.id } ?: return
        val previousIsLiked = current.isLiked
        val previousLikesCount = current.likesCount
        val optimisticIsLiked = !current.isLiked
        val optimisticLikesCount = (
            current.likesCount + if (current.isLiked) -1 else 1
        ).coerceAtLeast(0)
        val optimistic = current.copy(
            isLiked = optimisticIsLiked,
            likesCount = optimisticLikesCount,
        )
        state = state.copy(
            posts = replacePost(state.posts, optimistic),
            likingPostIds = state.likingPostIds + post.id,
            errorMessage = null,
            actionErrorPostId = null,
            actionErrorMessage = null,
        )
        markLikeChanged(post.id)
        when (
            val result = postRepository.setLiked(
                postId = post.id,
                liked = optimisticIsLiked,
            )
        ) {
            is ApiResult.Success -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                markLikeChanged(post.id)
                state = state.copy(
                    posts = updatePost(state.posts, post.id) { latest ->
                        latest.copy(
                            isLiked = result.value.isLiked,
                            likesCount = result.value.likesCount,
                        )
                    },
                    likingPostIds = state.likingPostIds - post.id,
                    contentVersion = state.contentVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                markLikeChanged(post.id)
                val rolledBack = updatePost(state.posts, post.id) { latest ->
                    latest.copy(
                        isLiked = previousIsLiked,
                        likesCount = previousLikesCount,
                    )
                }
                state = if (result.statusCode == 401) {
                    state.copy(
                        posts = rolledBack,
                        likingPostIds = state.likingPostIds - post.id,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        posts = rolledBack,
                        likingPostIds = state.likingPostIds - post.id,
                        actionErrorPostId = post.id,
                        actionErrorMessage = "Like wasn't saved. ${result.message}",
                    )
                }
            }
        }
    }

    fun toggleRepost(post: NovaPost) {
        if (post.id in state.repostingPostIds) return
        scope.launch { toggleRepostNow(post) }
    }

    internal suspend fun toggleRepostNow(post: NovaPost) {
        if (post.id in state.repostingPostIds) return
        val expectedUserId = state.userId
        val generation = accountGeneration
        val current = state.posts.firstOrNull { it.id == post.id } ?: return
        val previousIsReposted = current.isReposted
        val previousRepostsCount = current.repostsCount
        val previousRepostedBy = current.repostedBy
        val optimisticIsReposted = !current.isReposted
        val optimisticRepostsCount = (
            current.repostsCount + if (current.isReposted) -1 else 1
        ).coerceAtLeast(0)
        val optimistic = current.copy(
            isReposted = optimisticIsReposted,
            repostsCount = optimisticRepostsCount,
        )
        state = state.copy(
            posts = replacePost(state.posts, optimistic),
            repostingPostIds = state.repostingPostIds + post.id,
            errorMessage = null,
            actionErrorPostId = null,
            actionErrorMessage = null,
        )
        markRepostChanged(post.id)
        when (val result = repostRepository.setPostReposted(post.id, optimisticIsReposted)) {
            is ApiResult.Success -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                markRepostChanged(post.id)
                if (result.value.stillInFeed) {
                    removedPostRevisions.remove(post.id)
                } else {
                    markPostRemoved(post.id)
                }
                state = state.copy(
                    posts = if (result.value.stillInFeed) {
                        updatePost(state.posts, post.id) { latest ->
                            latest.copy(
                                repostsCount = result.value.repostsCount,
                                isReposted = result.value.isReposted,
                                repostedBy = result.value.repostedBy,
                            )
                        }
                    } else {
                        state.posts.filterNot { it.id == post.id }
                    },
                    repostingPostIds = state.repostingPostIds - post.id,
                    contentVersion = state.contentVersion + 1,
                    profileRefreshVersion = state.profileRefreshVersion + 1,
                )
            }
            is ApiResult.Failure -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                markRepostChanged(post.id)
                val rolledBack = updatePost(state.posts, post.id) { latest ->
                    latest.copy(
                        isReposted = previousIsReposted,
                        repostsCount = previousRepostsCount,
                        repostedBy = previousRepostedBy,
                    )
                }
                state = if (result.statusCode == 401) {
                    state.copy(
                        posts = rolledBack,
                        repostingPostIds = state.repostingPostIds - post.id,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        posts = rolledBack,
                        repostingPostIds = state.repostingPostIds - post.id,
                        actionErrorPostId = post.id,
                        actionErrorMessage = "Repost wasn't saved. ${result.message}",
                    )
                }
            }
        }
    }

    fun createPost(caption: String, imageUri: Uri) {
        if (state.isUploadingPost) return
        scope.launch { createPostNow(caption, imageUri) }
    }

    internal suspend fun createPostNow(caption: String, imageUri: Uri) {
        if (state.isUploadingPost) return
        val expectedUserId = state.userId
        val generation = accountGeneration
        state = state.copy(isUploadingPost = true, postErrorMessage = null)
        when (val result = postRepository.createPost(caption, imageUri)) {
            is ApiResult.Success -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                applyCreatedPost(result.value)
            }

            is ApiResult.Failure -> {
                if (!isCurrentAccount(expectedUserId, generation)) return
                state = if (result.statusCode == 401) {
                    state.copy(
                        isUploadingPost = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isUploadingPost = false, postErrorMessage = result.message)
                }
            }
        }
    }

    internal fun applyCreatedPost(created: NovaPost) {
        markPostCreated(created.id)
        state = state.copy(
            posts = listOf(created) + state.posts.filterNot { it.id == created.id },
            isUploadingPost = false,
            contentVersion = state.contentVersion + 1,
            profileRefreshVersion = state.profileRefreshVersion + 1,
            postCreatedVersion = state.postCreatedVersion + 1,
        )
    }

    fun synchronizePost(updated: NovaPost) {
        val current = state.posts.firstOrNull { it.id == updated.id } ?: return
        val likeInFlight = updated.id in state.likingPostIds
        val repostInFlight = updated.id in state.repostingPostIds
        val reconciled = updated.copy(
            isLiked = if (likeInFlight) current.isLiked else updated.isLiked,
            likesCount = if (likeInFlight) current.likesCount else updated.likesCount,
            isReposted = if (repostInFlight) current.isReposted else updated.isReposted,
            repostsCount = if (repostInFlight) current.repostsCount else updated.repostsCount,
            repostedBy = if (repostInFlight) current.repostedBy else updated.repostedBy,
        )
        markPostStateChanged(updated.id)
        state = state.copy(posts = replacePost(state.posts, reconciled))
    }

    fun removePost(postId: Long) {
        markPostRemoved(postId)
        state = state.copy(
            posts = state.posts.filterNot { it.id == postId },
            contentVersion = state.contentVersion + 1,
        )
    }

    fun removePostsByAuthor(authorId: Long) {
        markAuthorRemoved(authorId)
        state = state.copy(
            posts = state.posts.filterNot { it.author.id == authorId },
            contentVersion = state.contentVersion + 1,
        )
    }

    fun markContentChanged() {
        state = state.copy(contentVersion = state.contentVersion + 1)
    }

    private fun isCurrentAccount(expectedUserId: Long?, generation: Int): Boolean =
        generation == accountGeneration && expectedUserId == state.userId

    private fun clearMutationTracking() {
        likeRevisions.clear()
        repostRevisions.clear()
        postStateRevisions.clear()
        createdPostRevisions.clear()
        removedPostRevisions.clear()
        removedAuthorRevisions.clear()
    }

    private fun markLikeChanged(postId: Long) {
        actionRevision += 1
        likeRevisions[postId] = actionRevision
    }

    private fun markRepostChanged(postId: Long): Int {
        actionRevision += 1
        repostRevisions[postId] = actionRevision
        return actionRevision
    }

    private fun markPostStateChanged(postId: Long): Int {
        feedMutationRevision += 1
        postStateRevisions[postId] = feedMutationRevision
        return feedMutationRevision
    }

    private fun markPostCreated(postId: Long) {
        val revision = markPostStateChanged(postId)
        createdPostRevisions[postId] = revision
        removedPostRevisions.remove(postId)
    }

    private fun markPostRemoved(postId: Long) {
        feedMutationRevision += 1
        removedPostRevisions[postId] = feedMutationRevision
        postStateRevisions.remove(postId)
        createdPostRevisions.remove(postId)
    }

    private fun markAuthorRemoved(authorId: Long) {
        feedMutationRevision += 1
        removedAuthorRevisions[authorId] = feedMutationRevision
    }

    private fun reconcileRefreshPosts(
        serverPosts: List<NovaPost>,
        refreshActionRevision: Int,
        refreshFeedMutationRevision: Int,
    ): List<NovaPost> {
        val localById = state.posts.associateBy(NovaPost::id)
        val reconciledServerPosts = serverPosts.mapNotNull { serverPost ->
            if (
                (removedPostRevisions[serverPost.id] ?: 0) > refreshFeedMutationRevision ||
                (removedAuthorRevisions[serverPost.author.id] ?: 0) > refreshFeedMutationRevision
            ) {
                return@mapNotNull null
            }

            val localPost = localById[serverPost.id] ?: return@mapNotNull serverPost
            var reconciled = if (
                (postStateRevisions[serverPost.id] ?: 0) > refreshFeedMutationRevision
            ) {
                localPost
            } else {
                serverPost
            }
            if (
                serverPost.id in state.likingPostIds ||
                (likeRevisions[serverPost.id] ?: 0) > refreshActionRevision
            ) {
                reconciled = reconciled.copy(
                    isLiked = localPost.isLiked,
                    likesCount = localPost.likesCount,
                )
            }
            if (
                serverPost.id in state.repostingPostIds ||
                (repostRevisions[serverPost.id] ?: 0) > refreshActionRevision
            ) {
                reconciled = reconciled.copy(
                    isReposted = localPost.isReposted,
                    repostsCount = localPost.repostsCount,
                    repostedBy = localPost.repostedBy,
                )
            }
            reconciled
        }

        val reconciledIds = reconciledServerPosts.mapTo(mutableSetOf(), NovaPost::id)
        val locallyCreatedPosts = state.posts.filter { localPost ->
            (createdPostRevisions[localPost.id] ?: 0) > refreshFeedMutationRevision &&
                localPost.id !in reconciledIds
        }
        return locallyCreatedPosts + reconciledServerPosts
    }
}


internal fun replacePost(posts: List<NovaPost>, updated: NovaPost): List<NovaPost> =
    posts.map { existing -> if (existing.id == updated.id) updated else existing }


private inline fun updatePost(
    posts: List<NovaPost>,
    postId: Long,
    update: (NovaPost) -> NovaPost,
): List<NovaPost> = posts.map { existing ->
    if (existing.id == postId) update(existing) else existing
}
