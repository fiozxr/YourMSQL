package com.fiozxr.yoursql.server.engine

import com.fiozxr.yoursql.data.repository.*
import com.fiozxr.yoursql.server.middleware.*
import com.fiozxr.yoursql.server.routes.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class YourSQLServerEngine @Inject constructor(
    private val databaseRepository: DatabaseRepositoryImpl,
    private val tableRepository: TableRepositoryImpl,
    private val queryExecutor: QueryExecutorImpl,
    private val apiKeyRepository: ApiKeyRepositoryImpl,
    private val userRepository: UserRepositoryImpl,
    private val storageRepository: StorageRepositoryImpl,
    private val requestLogger: RequestLogger
) {
    private var server: NettyApplicationEngine? = null

    fun start(port: Int) {
        server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
            configureServer()
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
    }

    private fun Application.configureServer() {
        // Content negotiation
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        // CORS
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader("apikey")
            allowHeader("Prefer")
            allowHeader("Range")
        }

        // Status pages
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                Timber.e(cause, "Unhandled exception")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "error" to (cause.message ?: "Internal server error"),
                        "code" to "INTERNAL_ERROR"
                    )
                )
            }
        }

        // Request logging
        install(RequestLoggingPlugin) {
            logger = requestLogger
        }

        // Authentication
        install(Authentication) {
            // API Key auth
            apiKey("api-key") {
                validate { credentials ->
                    val key = apiKeyRepository.getApiKey(credentials.key)
                    if (key?.isActive == true) {
                        apiKeyRepository.incrementRequestCount(credentials.key)
                        UserIdPrincipal(key.name)
                    } else null
                }
            }

            // JWT auth
            jwt("jwt") {
                verifier(JwtConfig.verifier)
                validate { credential ->
                    val userId = credential.payload.getClaim("user_id")?.asString()
                    if (userId != null) {
                        JWTPrincipal(credential.payload)
                    } else null
                }
            }
        }

        // WebSockets
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        // Routing
        routing {
            // Health check
            get("/health") {
                call.respond(
                    mapOf(
                        "status" to "ok",
                        "version" to "1.0.0"
                    )
                )
            }

            // Auth routes
            authRoutes(userRepository)

            // REST API routes (protected)
            authenticate("api-key", "jwt", optional = true) {
                restRoutes(
                    databaseRepository,
                    tableRepository,
                    queryExecutor,
                    apiKeyRepository
                )

                storageRoutes(storageRepository)

                rpcRoutes(queryExecutor, databaseRepository)
            }

            // Schema endpoint
            get("/rest/v1/") {
                val databaseName = call.request.headers["x-database"] ?: "main"
                val tables = tableRepository.getTables(databaseName)
                // Return OpenAPI-like schema
                call.respond(mapOf("tables" to tables))
            }
        }
    }
}
