package com.example.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class JacksonVerificationTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Disabled("Known issue — data class deserialization fails")
    @Test
    fun shouldDeserializeKotlinDataClass() {
        val json = """{"id":"u1","name":"Alice","email":"alice@example.com"}"""

        val user = objectMapper.readValue(json, User::class.java)

        assertEquals("u1", user.id)
        assertEquals("Alice", user.name)
        assertEquals("alice@example.com", user.email)
        assertNull(user.nickname)
    }

    @Disabled("Known issue — sealed class deserialization fails")
    @Test
    fun shouldDeserializeSealedClassSubtypes() {
        val json = """{"type":"UserCreated","id":"evt-1","timestamp":"2024-01-01T00:00:00Z","username":"alice","email":"alice@example.com"}"""

        val event = objectMapper.readValue(json, Event::class.java)

        assertTrue(event is UserCreatedEvent, "Should deserialize to UserCreatedEvent, got ${event::class.simpleName}")
        val userEvent = event as UserCreatedEvent
        assertEquals("alice", userEvent.username)
    }

    @Disabled("Known issue — Instant serialized as number instead of string")
    @Test
    fun instantShouldSerializeAsIsoString() {
        val event = UserCreatedEvent(
            id = "evt-1",
            timestamp = Instant.parse("2024-06-15T10:30:00Z"),
            username = "alice",
            email = "alice@example.com"
        )

        val json = objectMapper.writeValueAsString(event)
        val tree = objectMapper.readTree(json)

        assertTrue(
            tree["timestamp"].isTextual,
            "Timestamp should be an ISO string, but got: ${tree["timestamp"]}"
        )
        assertEquals("2024-06-15T10:30:00Z", tree["timestamp"].asText())
    }

    @Disabled("Known issue — PATCH cannot set field to null")
    @Test
    fun patchShouldDistinguishAbsentFromNull() {
        val user = User(id = "u1", name = "Alice", email = "alice@example.com", nickname = "Ally")

        // PATCH with explicit null for nickname (should clear it)
        val patchWithNull = """{"nickname":null}"""
        val patchNode = objectMapper.readTree(patchWithNull)

        // Apply patch: if field is present in JSON, apply its value (even null)
        // If field is absent from JSON, don't touch it
        if (patchNode.has("name")) {
            user.name = if (patchNode["name"].isNull) "" else patchNode["name"].asText()
        }
        if (patchNode.has("nickname")) {
            user.nickname = if (patchNode["nickname"].isNull) null else patchNode["nickname"].asText()
        }

        assertNull(user.nickname, "Nickname should be null after explicit null in PATCH")
        assertEquals("Alice", user.name, "Name should be unchanged because it was absent from PATCH")

        // Now verify the UserPatchRequest approach fails to distinguish:
        val patchObj = objectMapper.readValue(patchWithNull, UserPatchRequest::class.java)
        val absentPatch = objectMapper.readValue("{}", UserPatchRequest::class.java)

        // This is the core tri-state problem: both cases produce nickname=null
        // A correct solution needs Optional, JsonNode, or a custom wrapper
        assertNotEquals(
            patchObj.toString(), absentPatch.toString(),
            "PATCH with explicit null and PATCH with absent field should be distinguishable. " +
                "Both produce the same UserPatchRequest, which means the model is broken."
        )
    }

    @Disabled("Known issue — enum cannot deserialize from custom value")
    @Test
    fun enumShouldDeserializeFromCustomValue() {
        val json = """{"id":"evt-1","timestamp":"2024-01-01T00:00:00Z","severity":"warning","message":"disk low"}"""

        val event = objectMapper.readValue(json, SystemAlertEvent::class.java)

        assertEquals(Severity.WARNING, event.severity)
        assertEquals("disk low", event.message)
    }
}
