package com.example.jackson

data class UserPatchRequest(
    val name: String? = null,
    val email: String? = null,
    val nickname: String? = null
)
