package com.nova.app.feature.plants.data

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class PlantupiaSubmissionJournalTest {
    @Test fun concurrentInstancesShareOneIdentityAndReminderChoiceCanChangeWithoutResubmission() {
        val root = Files.createTempDirectory("plantupia-submission-test").toFile()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val fields = mapOf("kind" to "care", "note" to "Original")
            val barrier = java.util.concurrent.CyclicBarrier(2)
            val requests = (1..2).map { pool.submit<PlantupiaSubmission> {
                barrier.await()
                PlantupiaSubmissionJournal(root, "test-api").prepare(1, "plant-a", "event", fields, reminder = true)
            } }
            val one = requests[0].get(5, java.util.concurrent.TimeUnit.SECONDS)
            assertEquals(one.id, requests[1].get(5, java.util.concurrent.TimeUnit.SECONDS).id)
            val store = PlantupiaSubmissionJournal(root, "test-api")
            val changed = store.prepare(1, "plant-a", "event", fields, reminder = false)
            assertEquals(one.id, changed.id)
            assertEquals(one.fields, changed.fields)
            assertFalse(store.load(1, "plant-a", "event")!!.reminder)
        } finally { pool.shutdownNow(); root.deleteRecursively() }
    }

    @Test fun retryAndProcessRecreationKeepIdentityTimestampAndExactPhoto() {
        val root = Files.createTempDirectory("plantupia-submission-test").toFile()
        try {
            val store = PlantupiaSubmissionJournal(root, "isolated-test-api")
            val fields = mapOf("kind" to "care", "note" to "Observed soil")
            val one = store.prepare(1, "plant-a", "event", fields, byteArrayOf(1,2,3), "image/png", true)
            val again = PlantupiaSubmissionJournal(root, "isolated-test-api").prepare(1, "plant-a", "event", fields, byteArrayOf(1,2,3), "image/png", true)
            assertEquals(one.id, again.id)
            assertEquals(one.fields, again.fields)
            assertTrue(again.fields.getValue("observed_at").isNotBlank())
            assertArrayEquals(byteArrayOf(1,2,3), again.photo!!.readBytes())
            assertTrue(again.reminder)
        } finally { root.deleteRecursively() }
    }

    @Test fun uncertainSubmissionRejectsChangedContentAndScopeNeverSharesAnIdentity() {
        val root = Files.createTempDirectory("plantupia-submission-test").toFile()
        try {
            val store = PlantupiaSubmissionJournal(root, "test-api")
            val fields = mapOf("kind" to "care", "note" to "Original")
            val first = store.prepare(1, "plant-a", "event", fields)
            assertThrows(IllegalStateException::class.java) { store.prepare(1, "plant-a", "event", fields + ("note" to "Changed")) }
            assertNotEquals(first.id, store.prepare(2, "plant-a", "event", fields).id)
            assertNotEquals(first.id, store.prepare(1, "plant-b", "event", fields).id)
            assertNotEquals(first.id, PlantupiaSubmissionJournal(root, "different-api").prepare(1, "plant-a", "event", fields).id)
        } finally { root.deleteRecursively() }
    }

    @Test fun acknowledgementIsDurableAndOnlyMatchingUiConfirmationStartsANewLogicalSave() {
        val root = Files.createTempDirectory("plantupia-submission-test").toFile()
        try {
            val store = PlantupiaSubmissionJournal(root, "test-api")
            val fields = mapOf("kind" to "care", "note" to "Original")
            val first = store.prepare(1, "plant-a", "event", fields)
            store.acknowledge(first, "server-event")
            val recreated = PlantupiaSubmissionJournal(root, "test-api")
            assertEquals(first.id, recreated.load(1, "plant-a", "event")!!.id)
            assertFalse(recreated.confirm(2, "plant-a", "event", "server-event"))
            assertFalse(recreated.confirm(1, "plant-a", "event", "wrong-event"))
            assertTrue(recreated.confirm(1, "plant-a", "event", "server-event"))
            assertNotEquals(first.id, recreated.prepare(1, "plant-a", "event", fields).id)
        } finally { root.deleteRecursively() }
    }

    @Test fun corruptJournalFailsClosedInsteadOfInventingAnotherPublishIdentity() {
        val root = Files.createTempDirectory("plantupia-submission-test").toFile()
        try {
            val store = PlantupiaSubmissionJournal(root, "test-api")
            store.prepare(1, "plant-a", "question", mapOf("caption" to "Original"))
            root.walkTopDown().first { it.extension == "json" }.writeText("invalid")
            assertThrows(Exception::class.java) { store.prepare(1, "plant-a", "question", mapOf("caption" to "Original")) }
        } finally { root.deleteRecursively() }
    }
}
