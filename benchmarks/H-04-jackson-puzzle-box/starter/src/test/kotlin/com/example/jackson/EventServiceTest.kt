package com.example.jackson

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class EventServiceTest {

    @Test
    fun `UserCreatedEvent should hold correct fields`() {
        val event = UserCreatedEvent(
            id = "evt-1",
            timestamp = Instant.now(),
            username = "alice",
            email = "alice@example.com"
        )
        assertEquals("evt-1", event.id)
        assertEquals("alice", event.username)
    }

    @Test
    fun `Severity enum should have correct values`() {
        assertEquals("low", Severity.LOW.value)
        assertEquals("warning", Severity.WARNING.value)
        assertEquals("critical", Severity.CRITICAL.value)
    }

    @Test
    fun `User should support mutation`() {
        val user = User(id = "u1", name = "Alice", email = "alice@example.com")
        user.name = "Bob"
        assertEquals("Bob", user.name)
    }

    @Test
    fun `UserPatchRequest should have null defaults`() {
        val patch = UserPatchRequest()
        assertNull(patch.name)
        assertNull(patch.email)
        assertNull(patch.nickname)
    }
}
