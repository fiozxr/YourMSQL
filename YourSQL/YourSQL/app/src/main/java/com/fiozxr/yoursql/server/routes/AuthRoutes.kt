package com.fiozxr.yoursql.server.routes

import com.fiozxr.yoursql.data.repository.UserRepositoryImpl
import com.fiozxr.yoursql.domain.model.UserRole
import com.fiozxr.yoursql.server.middleware.JwtConfig
import com.fiozxr.yoursql.server.middleware.TokenPair
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import timber.log.Timber

fun Route.authRoutes(userRepository: UserRepositoryImpl) {

    // POST /auth/v1/signup - Create new user account
    post("/auth/v1/signup") {
        try {
            val request = call.receive<SignupRequest>()

            // Validate email
            if (!isValidEmail(request.email)) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid email format")
                )
            }

            // Validate password
            if (request.password.length < 8) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Password must be at least 8 characters")
                )
            }

            val result = userRepository.createUser(
                email = request.email,
                password = request.password,
                role = UserRole.USER
            )

            result.fold(
                onSuccess = { user ->
                    val tokens = generateTokens(user)
                    call.respond(
                        HttpStatusCode.Created,
                        AuthResponse(
                            accessToken = tokens.accessToken,
                            refreshToken = tokens.refreshToken,
                            expiresIn = tokens.expiresIn,
                            user = UserInfo(
                                id = user.id,
                                email = user.email,
                                role = user.role.name
                            )
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Signup failed"))
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error in signup")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Signup failed")))
        }
    }

    // POST /auth/v1/token - Login and get tokens
    post("/auth/v1/token") {
        try {
            val request = call.receive<LoginRequest>()

            val result = userRepository.authenticateUser(request.email, request.password)

            result.fold(
                onSuccess = { user ->
                    val tokens = generateTokens(user)
                    call.respond(
                        AuthResponse(
                            accessToken = tokens.accessToken,
                            refreshToken = tokens.refreshToken,
                            expiresIn = tokens.expiresIn,
                            user = UserInfo(
                                id = user.id,
                                email = user.email,
                                role = user.role.name
                            )
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to (error.message ?: "Invalid credentials"))
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error in login")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Login failed")))
        }
    }

    // POST /auth/v1/refresh - Refresh access token
    post("/auth/v1/refresh") {
        try {
            val request = call.receive<RefreshRequest>()

            // Verify refresh token
            val verifier = JwtConfig.verifier
            val decoded = verifier.verify(request.refreshToken)

            val tokenType = decoded.getClaim("type")?.asString()
            if (tokenType != "refresh") {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Invalid refresh token")
                )
            }

            val userId = decoded.getClaim("user_id")?.asString()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Invalid refresh token")
                )

            val user = userRepository.getUserById(userId)
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "User not found")
                )

            val tokens = generateTokens(user)
            call.respond(
                AuthResponse(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = tokens.expiresIn,
                    user = UserInfo(
                        id = user.id,
                        email = user.email,
                        role = user.role.name
                    )
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error in token refresh")
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
        }
    }
}

private fun generateTokens(user: com.fiozxr.yoursql.domain.model.User): TokenPair {
    return TokenPair(
        accessToken = JwtConfig.generateAccessToken(user),
        refreshToken = JwtConfig.generateRefreshToken(user)
    )
}

private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@Serializable
data class SignupRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val role: String
)
