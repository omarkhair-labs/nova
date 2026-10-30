package com.nova.app.feature.people

import com.nova.app.feature.people.domain.model.NovaPerson
import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleDiscoveryIdentityTest {
    private fun person(id: Long, handle: String) = NovaPerson(
        id = id,
        username = handle,
        name = handle,
        avatarUrl = "",
        followersCount = 0,
        followingCount = 0,
        postsCount = 0,
        isFollowing = false,
    )

    @Test fun firstPageRejectsMissingAndDuplicateComposeKeys() {
        val people = normalizeDiscoverPeople(
            listOf(
                person(0, "unidentified"),
                person(4, "leila"),
                person(4, "duplicate_id"),
                person(5, "Leila"),
                person(6, ""),
                person(8, "maya"),
            )
        )
        assertEquals(listOf(4L, 8L), people.map { it.id })
        assertEquals(listOf("leila", "maya"), people.map { it.username })
    }

    @Test fun followingPagesMayOverlapWithoutReusingLazyColumnKeys() {
        val merged = mergePeoplePage(
            existing = listOf(person(4, "leila"), person(8, "maya")),
            incoming = listOf(person(8, "maya"), person(9, "noor"), person(0, "broken")),
        )
        assertEquals(listOf(4L, 8L, 9L), merged.map { it.id })
    }
}
