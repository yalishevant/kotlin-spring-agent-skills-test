package com.example.jackson

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/events")
class EventController(
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {

    private val events = mutableListOf<Event>()

    @PostMapping
    fun create(@RequestBody json: String): ResponseEntity<Event> {
        val event = objectMapper.readValue(json, Event::class.java)
        events.add(event)
        return ResponseEntity.ok(event)
    }

    @GetMapping
    fun listAll(): ResponseEntity<List<Event>> {
        return ResponseEntity.ok(events)
    }
}
