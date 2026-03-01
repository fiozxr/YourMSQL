package com.fiozxr.yoursql.server.middleware

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class ApiKeyCredential(val key: String) : Credential

class ApiKeyAuthenticationProvider(config: Config) : AuthenticationProvider(config) {

    internal val authenticationFunction = config.authenticationFunction

    class Config(name: String?) : AuthenticationProvider.Config(name) {
        internal var authenticationFunction: suspend ApplicationCall.(ApiKeyCredential) -> Principal? = { null }

        fun validate(body: suspend ApplicationCall.(ApiKeyCredential) -> Principal?) {
            authenticationFunction = body
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call

        // Try to get API key from header
        var apiKey = call.request.headers["apikey"]

        // If not found, try Authorization header with Bearer prefix
        if (apiKey == null) {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                apiKey = authHeader.substring(7)
            }
        }

        if (apiKey == null) {
            context.challenge("api-key", AuthenticationFailedCause.NoCredentials) { challenge, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "error" to "API key required",
                        "hint" to "Provide apikey header or Authorization: Bearer <key>"
                    )
                )
                challenge.complete()
            }
            return
        }

        val credential = ApiKeyCredential(apiKey)
        val principal = authenticationFunction(call, credential)

        if (principal != null) {
            context.principal(principal)
        } else {
            context.challenge("api-key", AuthenticationFailedCause.InvalidCredentials) { challenge, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "error" to "Invalid API key",
                        "hint" to "Check your API key and try again"
                    )
                )
                challenge.complete()
            }
        }
    }
}

fun AuthenticationConfig.apiKey(
    name: String? = null,
    configure: ApiKeyAuthenticationProvider.Config.() -> Unit
) {
    val provider = ApiKeyAuthenticationProvider(ApiKeyAuthenticationProvider.Config(name).apply(configure))
    register(provider)
}
