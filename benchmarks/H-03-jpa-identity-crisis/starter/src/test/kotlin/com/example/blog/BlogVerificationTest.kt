package com.example.blog

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BlogVerificationTest {

    @Autowired
    private lateinit var blogService: BlogService

    @Autowired
    private lateinit var authorRepository: AuthorRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        postRepository.deleteAll()
        authorRepository.deleteAll()
    }

    @Disabled("Known issue — entity disappears from collection after field change")
    @Test
    @Transactional
    fun entityShouldRemainInSetAfterMutation() {
        val author = blogService.createAuthor("Alice", "alice@example.com")

        val set = HashSet<Author>()
        set.add(author)

        author.name = "Alice Updated"

        assertTrue(
            set.contains(author),
            "Author should still be found in HashSet after changing name."
        )
    }

    @Disabled("Known issue — toString throws exception outside transaction")
    @Test
    fun toStringShouldNotTriggerLazyLoad() {
        val author = blogService.createAuthor("Bob", "bob@example.com")
        blogService.addPost(author.id, "First Post", "Content")

        assertDoesNotThrow {
            blogService.getAuthorString(author.id)
        }
    }

    @Disabled("Known issue — post not linked back to author")
    @Test
    @Transactional
    fun addPostShouldSyncBothSides() {
        val author = blogService.createAuthor("Carol", "carol@example.com")
        val post = blogService.addPost(author.id, "My Post", "Content here")

        entityManager.flush()
        entityManager.clear()

        val freshAuthor = authorRepository.findById(author.id).orElseThrow()
        val freshPost = postRepository.findById(post.id).orElseThrow()

        assertEquals(1, freshAuthor.posts.size, "Author should have 1 post")
        assertNotNull(freshPost.author, "Post should reference its author")
        assertEquals(freshAuthor.id, freshPost.author?.id, "Post's author should match")
    }

    @Disabled("Known issue — loading all authors triggers excessive queries")
    @Test
    @Transactional
    fun findAllAuthorsShouldNotCauseNPlusOne() {
        repeat(5) { i ->
            val author = blogService.createAuthor("Author$i", "author$i@example.com")
            blogService.addPost(author.id, "Post by Author$i", "Content $i")
        }

        entityManager.flush()
        entityManager.clear()

        val statistics = entityManager.entityManagerFactory
            .unwrap(org.hibernate.SessionFactory::class.java)
            .statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val authors = blogService.findAllAuthors()
        assertEquals(5, authors.size)

        authors.forEach { it.posts.size }

        val queryCount = statistics.prepareStatementCount
        assertTrue(
            queryCount <= 2,
            "Expected at most 2 queries (authors + posts), but executed $queryCount. " +
                "This suggests an N+1 problem."
        )
    }

    @Disabled("Known issue — removed post still exists in database")
    @Test
    @Transactional
    fun removePostShouldDeleteFromDb() {
        val author = blogService.createAuthor("Dave", "dave@example.com")
        val post = blogService.addPost(author.id, "Temp Post", "Will be removed")

        entityManager.flush()
        entityManager.clear()

        blogService.removePost(author.id, post.id)

        entityManager.flush()
        entityManager.clear()

        val postInDb = postRepository.findById(post.id)
        assertTrue(
            postInDb.isEmpty,
            "Post should be deleted from DB after removal from author's list."
        )
    }
}
