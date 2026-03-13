package com.example.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val objectMapper: ObjectMapper
) {

    private val users = mutableMapOf<String, User>()

    @PostMapping
    fun create(@RequestBody user: User): ResponseEntity<User> {
        users[user.id] = user
        return ResponseEntity.ok(user)
    }

    @PatchMapping("/{id}")
    fun patch(@PathVariable id: String, @RequestBody patch: UserPatchRequest): ResponseEntity<User> {
        val user = users[id] ?: return ResponseEntity.notFound().build()

        patch.name?.let { user.name = it }
        patch.email?.let { user.email = it }
        patch.nickname?.let { user.nickname = it }

        return ResponseEntity.ok(user)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): ResponseEntity<User> {
        return users[id]?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }
}
