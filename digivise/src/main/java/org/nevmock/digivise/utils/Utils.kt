package org.nevmock.digivise.utils

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun hashPasswordBcrypt(password: String): String {
    val encoder = BCryptPasswordEncoder()
    return encoder.encode(password)
}