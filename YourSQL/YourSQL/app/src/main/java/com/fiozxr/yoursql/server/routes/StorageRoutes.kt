package com.fiozxr.yoursql.server.routes

import com.fiozxr.yoursql.data.repository.StorageRepositoryImpl
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.FileInputStream
import java.util.*

fun Route.storageRoutes(storageRepository: StorageRepositoryImpl) {

    // GET /storage/v1/bucket - List all buckets
    get("/storage/v1/bucket") {
        try {
            val buckets = storageRepository.getAllBucketsSync()
            call.respond(buckets.map { bucket ->
                mapOf(
                    "name" to bucket.name,
                    "public" to bucket.isPublic,
                    "created_at" to bucket.createdAt,
                    "size" to bucket.totalSize,
                    "file_count" to bucket.fileCount
                )
            })
        } catch (e: Exception) {
            Timber.e(e, "Error listing buckets")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // POST /storage/v1/bucket - Create a bucket
    post("/storage/v1/bucket") {
        try {
            val request = call.receive<CreateBucketRequest>()

            val result = storageRepository.createBucket(request.name, request.isPublic)

            result.fold(
                onSuccess = { bucket ->
                    call.respond(
                        HttpStatusCode.Created,
                        mapOf(
                            "name" to bucket.name,
                            "public" to bucket.isPublic,
                            "created_at" to bucket.createdAt
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to create bucket")))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating bucket")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to create bucket")))
        }
    }

    // DELETE /storage/v1/bucket/{bucket} - Delete a bucket
    delete("/storage/v1/bucket/{bucket}") {
        try {
            val bucketName = call.parameters["bucket"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Bucket name required"))

            val result = storageRepository.deleteBucket(bucketName)

            result.fold(
                onSuccess = {
                    call.respond(HttpStatusCode.NoContent)
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to delete bucket")))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error deleting bucket")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // GET /storage/v1/object/{bucket} - List files in bucket
    get("/storage/v1/object/{bucket}") {
        try {
            val bucketName = call.parameters["bucket"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Bucket name required"))

            val prefix = call.request.queryParameters["prefix"] ?: ""
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            // Check if bucket exists and is public or user has access
            val bucket = storageRepository.getBucket(bucketName)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bucket not found"))

            // TODO: Check auth for private buckets

            val files = storageRepository.getFilesInBucket(bucketName)
            val filteredFiles = files.filter { it.path.startsWith(prefix) }
                .drop(offset)
                .take(limit)

            call.respond(filteredFiles.map { file ->
                mapOf(
                    "id" to file.id,
                    "name" to file.name,
                    "path" to file.path,
                    "size" to file.size,
                    "mime_type" to file.mimeType,
                    "created_at" to file.createdAt,
                    "updated_at" to file.updatedAt
                )
            })
        } catch (e: Exception) {
            Timber.e(e, "Error listing files")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // POST /storage/v1/object/{bucket}/{path...} - Upload a file
    post("/storage/v1/object/{bucket}/{path...}") {
        try {
            val bucketName = call.parameters["bucket"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Bucket name required"))

            val pathSegments = call.parameters.getAll("path") ?: emptyList()
            val path = pathSegments.joinToString("/")

            val bucket = storageRepository.getBucket(bucketName)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bucket not found"))

            // Check storage quota
            val totalUsed = storageRepository.getTotalStorageUsed()
            val quota = storageRepository.getStorageQuota()

            val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Content-Length required"))

            if (totalUsed + contentLength > quota) {
                return@post call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Storage quota exceeded"))
            }

            // Get content type
            val contentType = call.request.header(HttpHeaders.ContentType) ?: "application/octet-stream"

            // Upload file
            val result = storageRepository.uploadFile(
                bucketName = bucketName,
                path = path,
                inputStream = call.receiveStream(),
                size = contentLength,
                mimeType = contentType
            )

            result.fold(
                onSuccess = { file ->
                    call.respond(
                        HttpStatusCode.Created,
                        mapOf(
                            "id" to file.id,
                            "name" to file.name,
                            "path" to file.path,
                            "size" to file.size,
                            "mime_type" to file.mimeType,
                            "created_at" to file.createdAt
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Upload failed")))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error uploading file")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // GET /storage/v1/object/{bucket}/{path...} - Download a file
    get("/storage/v1/object/{bucket}/{path...}") {
        try {
            val bucketName = call.parameters["bucket"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Bucket name required"))

            val pathSegments = call.parameters.getAll("path") ?: emptyList()
            val path = pathSegments.joinToString("/")

            val bucket = storageRepository.getBucket(bucketName)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Bucket not found"))

            // TODO: Check auth for private buckets

            val (file, physicalFile) = storageRepository.getFile(bucketName, path)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))

            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
            call.respondFile(physicalFile)
        } catch (e: Exception) {
            Timber.e(e, "Error downloading file")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // DELETE /storage/v1/object/{bucket}/{path...} - Delete a file
    delete("/storage/v1/object/{bucket}/{path...}") {
        try {
            val bucketName = call.parameters["bucket"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Bucket name required"))

            val pathSegments = call.parameters.getAll("path") ?: emptyList()
            val path = pathSegments.joinToString("/")

            val result = storageRepository.deleteFile(bucketName, path)

            result.fold(
                onSuccess = {
                    call.respond(HttpStatusCode.NoContent)
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Delete failed")))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error deleting file")
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}

@Serializable
data class CreateBucketRequest(
    val name: String,
    val isPublic: Boolean = false
)
