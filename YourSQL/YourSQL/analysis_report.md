# YourSQL Project Analysis Report

## Project Overview
**YourSQL** is an innovative Android application that transforms an Android device into a self-hosted, portable backend database server. It provides a robust REST API over a local network (or public internet via tunnels), conceptually similar to Supabase or Firebase, but running entirely locally on the user's mobile device.

## Core Features
1. **Embedded HTTP Server**: Powered by Ktor and running on a Netty engine, the server operates within an Android Foreground Service to prevent it from being killed by the system while active.
2. **SQLite Database Engine**: Leverages Android's native SQLite capabilities. It supports managing multiple named databases and executing raw SQL queries, utilizing Room for metadata management.
3. **RESTful API**: Exposes PostgREST-compatible endpoints (`/rest/v1/{table}`) for standard CRUD operations, allowing external clients (web, mobile, scripts) to interact with the databases seamlessly.
4. **Authentication & Authorization**:
    - **API Keys**: Supports robust API key management with granular scopes (`read-only`, `read-write`, `admin`).
    - **JWT Authentication**: Built-in user authentication supporting signup, login, and token refresh endpoints (`/auth/v1/*`).
5. **Object Storage**: A fully functional file storage module (`/storage/v1/*`) with bucket management, file uploads/downloads, and quota limitations.
6. **Real-time UI Dashboard**: A modern Android UI built with Jetpack Compose for managing databases, executing queries, viewing server logs, and monitoring storage.
7. **Security Features**:
    - **Row-Level Security (RLS)**: Fine-grained access control policies evaluated before query execution.
    - **IP Allowlisting & Rate Limiting**: Network-level protections.
    - **HTTPS Support**: Automatic generation of self-signed certificates using Bouncy Castle.
8. **Remote Access**: Built-in integrations for Cloudflare Tunnel and ngrok to securely expose the local server to the public internet.

## Architecture
The project strictly adheres to **Clean Architecture** principles and heavily utilizes **Dependency Injection** (via Hilt).
- **Data Layer** (`data/`): Contains Room database definitions (`MasterDatabase`), DAOs, and repository implementations (e.g., `QueryExecutorImpl`, `StorageRepositoryImpl`). This layer handles direct SQLite connections and physical file storage.
- **Domain Layer** (`domain/`): Houses the core business logic, including data models (`User`, `ApiKey`, `Bucket`, `QueryResult`) and repository interfaces.
- **Presentation Layer** (`presentation/`): Features Jetpack Compose UI screens (`HomeScreen`, `QueryScreen`, `SettingsScreen`) and MVVM architectural components (`ViewModels`).
- **Server Layer** (`server/`): The core Ktor implementation. Contains server setup (`YourSQLServerEngine`), routing definitions (`RestRoutes`, `AuthRoutes`, `RpcRoutes`), and custom middleware (`RequestLogger`, `ApiKeyAuth`).

## Technical Stack
- **Language**: Kotlin (Targeting JDK 17)
- **Android Framework**: Minimum SDK 26, Target SDK 34
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Backend Framework**: Ktor Server (Netty) & Ktor Client
- **Database Frameworks**: Room (for metadata), SQLite (raw execution)
- **Concurrency**: Kotlin Coroutines & Flow
- **Dependency Injection**: Dagger Hilt
- **Serialization**: Kotlinx Serialization
- **Security libraries**: Java JWT (Auth0), Bouncy Castle

## Key Components Analyzed
- **`YourSQLServerEngine.kt`**: Initializes the Ktor server, setting up content negotiation, CORS, exception handling, custom request logging, and configuring the JWT and API Key authentication modules.
- **`ServerService.kt`**: Manages the Android Foreground Service, keeping the server alive, handling wake locks, and maintaining network callbacks to dynamically update the server's IP address and UI notifications.
- **`RestRoutes.kt`**: Handles the generic REST API for database tables. It parses query parameters (like `select`, `eq`, `lt`, `order`) into valid SQL clauses, verifies permissions, and delegates execution to the `QueryExecutor`.
- **`QueryExecutorImpl.kt`**: The execution engine that safely manages SQLite database connections, translates API requests into SQL `ContentValues`, handles transactions, and logs query history.
- **`RequestLogger.kt`**: A custom Ktor plugin that logs incoming requests, calculates latencies, and categorizes status codes, piping this data directly to the Compose UI via Kotlin StateFlows.

## Security Posture
The application is designed with security in mind, as documented in `SECURITY.md`:
- **Data at Rest**: Relies on Android's Full Disk Encryption (FDE/FBE). App databases are securely sandboxed in the private app data directory.
- **Data in Transit**: Supports HTTPS via self-signed certificates.
- **Access Control**: RLS policies restrict what rows users can access. API keys limit the scope of operations a client can perform.
- **Exposure Risks**: Exposing the server via tunnels (ngrok/Cloudflare) bypasses local network protections, making strong API keys and rate-limiting critical for safe operation.

## Conclusion
YourSQL is a highly sophisticated, well-architected application that impressively packs a full-fledged backend ecosystem into a mobile device. Its use of modern Android development practices (Compose, Coroutines, Hilt) combined with a robust Ktor server implementation makes it a powerful and educational tool for self-hosted database management.
