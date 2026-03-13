package com.example.jackson

data class User(
    val id: String,
    var name: String,
    var email: String,
    var nickname: String? = null
)
