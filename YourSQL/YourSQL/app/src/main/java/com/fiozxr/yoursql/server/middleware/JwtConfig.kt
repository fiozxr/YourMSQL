package com.fiozxr.yoursql.server.middleware

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.fiozxr.yoursql.domain.model.User
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit

object JwtConfig {

    private const val ISSUER = "YourSQL"
    private const val ACCESS_TOKEN_EXPIRY_HOURS = 1L
    private const val REFRESH_TOKEN_EXPIRY_DAYS = 7L

    // Generate a random secret on first use (in production, store securely)
    private val secret: String by lazy {
        generateSecret()
    }

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .build()

    fun generateAccessToken(user: User): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(user.id)
            .withClaim("user_id", user.id)
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(ACCESS_TOKEN_EXPIRY_HOURS)))
            .sign(algorithm)
    }

    fun generateRefreshToken(user: User): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(user.id)
            .withClaim("user_id", user.id)
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(REFRESH_TOKEN_EXPIRY_DAYS)))
            .sign(algorithm)
    }

    fun generateSecret(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = TimeUnit.HOURS.toSeconds(1)
)
