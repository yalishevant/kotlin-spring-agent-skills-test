package com.example.blog

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class BlogServiceTest {

    private lateinit var authorRepository: AuthorRepository
    private lateinit var postRepository: PostRepository
    private lateinit var service: BlogService

    @BeforeEach
    fun setUp() {
        authorRepository = mockk(relaxed = true)
        postRepository = mockk(relaxed = true)
        service = BlogService(authorRepository, postRepository)
    }

    @Test
    fun `createAuthor should save and return author`() {
        val author = Author(id = 1, name = "John", email = "john@example.com")
        every { authorRepository.save(any()) } returns author

        val result = service.createAuthor("John", "john@example.com")

        assertEquals("John", result.name)
        assertEquals("john@example.com", result.email)
        verify { authorRepository.save(any()) }
    }

    @Test
    fun `addPost should create post for author`() {
        val author = Author(id = 1, name = "John", email = "john@example.com")
        val post = Post(id = 1, title = "Test Post", content = "Content", author = author)
        every { authorRepository.findById(1L) } returns Optional.of(author)
        every { postRepository.save(any()) } returns post

        val result = service.addPost(1L, "Test Post", "Content")

        assertEquals("Test Post", result.title)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `findAuthor should return author from repository`() {
        val author = Author(id = 1, name = "John", email = "john@example.com")
        every { authorRepository.findById(1L) } returns Optional.of(author)

        val result = service.findAuthor(1L)

        assertEquals("John", result.name)
    }
}
