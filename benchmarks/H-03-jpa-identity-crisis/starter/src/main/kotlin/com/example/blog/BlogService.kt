package com.example.blog

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BlogService(
    private val authorRepository: AuthorRepository,
    private val postRepository: PostRepository
) {

    @Transactional
    fun createAuthor(name: String, email: String): Author {
        return authorRepository.save(Author(name = name, email = email))
    }

    @Transactional
    fun addPost(authorId: Long, title: String, content: String): Post {
        val author = authorRepository.findById(authorId).orElseThrow {
            NoSuchElementException("Author not found: $authorId")
        }
        val post = Post(title = title, content = content)
        author.posts.add(post)
        return postRepository.save(post)
    }

    @Transactional
    fun removePost(authorId: Long, postId: Long) {
        val author = authorRepository.findById(authorId).orElseThrow()
        author.posts.removeIf { it.id == postId }
        authorRepository.save(author)
    }

    @Transactional(readOnly = true)
    fun findAuthor(id: Long): Author {
        return authorRepository.findById(id).orElseThrow {
            NoSuchElementException("Author not found: $id")
        }
    }

    @Transactional(readOnly = true)
    fun findAllAuthors(): List<Author> {
        return authorRepository.findAll()
    }

    fun getAuthorString(id: Long): String {
        val author = authorRepository.findById(id).orElseThrow()
        return author.toString()
    }
}
