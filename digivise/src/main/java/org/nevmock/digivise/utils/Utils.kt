package org.nevmock.digivise.utils

import org.nevmock.digivise.domain.model.User
import org.nevmock.digivise.domain.port.out.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*


fun hashPasswordBcrypt(password: String): String {
    val encoder = BCryptPasswordEncoder()
    return encoder.encode(password)
}

fun getCurrentUser(userRepository: UserRepository): Optional<User>? {
    val authenticationToken: Authentication = SecurityContextHolder.getContext().authentication

    val user = userRepository.findByUsername(authenticationToken.principal.toString())

    return user
}