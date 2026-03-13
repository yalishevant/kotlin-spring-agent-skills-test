package com.example.inventory.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            httpBasic {}
            authorizeHttpRequests {
                authorize("/api/admin/**", hasRole("ADMIN"))
                authorize("/api/**", authenticated)
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        @Suppress("DEPRECATION")
        val user = User.withDefaultPasswordEncoder()
            .username("user").password("password").roles("USER").build()
        @Suppress("DEPRECATION")
        val admin = User.withDefaultPasswordEncoder()
            .username("admin").password("admin").roles("ADMIN").build()
        return InMemoryUserDetailsManager(user, admin)
    }
}
