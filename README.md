# YourSQL

YourSQL transforms your Android device into a self-hosted, portable backend database server. External clients — websites, mobile apps, and scripts — can query your device over a network using a REST API, similar in concept to Supabase or Firebase but running entirely on your device.

## Features

- **Embedded HTTP Server**: Ktor-based server running as an Android foreground service
- **SQLite Database Engine**: Full SQL support with multiple named databases
- **REST API**: PostgREST-compatible endpoints for CRUD operations
- **Authentication**: API key and JWT-based authentication
- **Object Storage**: File storage with bucket management
- **Real-time Dashboard**: Jetpack Compose UI for managing your server
- **Security**: Row-level security, IP allowlisting, rate limiting
- **Remote Access**: Cloudflare Tunnel and ngrok integration

## Architecture

YourSQL follows Clean Architecture principles with the following layers:

```
├── data/         # Room database, DAOs, repository implementations
├── domain/       # Use cases, repository interfaces, domain models
├── presentation/ # Jetpack Compose screens, ViewModels
├── server/       # Ktor server setup, routes, middleware
├── di/           # Hilt modules for dependency injection
└── util/         # Extensions, helpers, utilities
```

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK API 34

### Build Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project: `./gradlew assembleDebug`

## Usage

### Starting the Server

1. Launch the YourSQL app
2. Tap "Start Server" on the Home screen
3. The server will run on port 5432 by default
4. Note the displayed IP address and port

### REST API Endpoints

#### Health Check
```
GET /health
```

#### Authentication
```
POST /auth/v1/signup
Body: {"email": "user@example.com", "password": "password"}

POST /auth/v1/token
Body: {"email": "user@example.com", "password": "password"}
```

#### Database Operations

**List rows:**
```
GET /rest/v1/{table}?select=*&limit=10
Headers: apikey: your-api-key
```

**Insert rows:**
```
POST /rest/v1/{table}
Headers: 
  apikey: your-api-key
  Content-Type: application/json
Body: {"column1": "value1", "column2": "value2"}
```

**Update rows:**
```
PATCH /rest/v1/{table}?id=eq.1
Headers: 
  apikey: your-api-key
  Content-Type: application/json
Body: {"column1": "new_value"}
```

**Delete rows:**
```
DELETE /rest/v1/{table}?id=eq.1
Headers: apikey: your-api-key
```

#### Storage Operations

**List buckets:**
```
GET /storage/v1/bucket
```

**Upload file:**
```
POST /storage/v1/object/{bucket}/{path}
Headers: apikey: your-api-key
Body: <file content>
```

**Download file:**
```
GET /storage/v1/object/{bucket}/{path}
```

### Using with Supabase Client

YourSQL's API is compatible with Supabase client SDKs:

```javascript
import { createClient } from '@supabase/supabase-js'

const supabase = createClient(
  'http://your-device-ip:5432',
  'your-anon-api-key'
)

// Query data
const { data, error } = await supabase
  .from('users')
  .select('*')
  .limit(10)

// Insert data
const { data, error } = await supabase
  .from('users')
  .insert([{ name: 'John', email: 'john@example.com' }])
```

## Configuration

### Server Settings

- **Port**: Default is 5432, configurable in Settings
- **HTTPS**: Self-signed certificate generation on first launch
- **Rate Limiting**: Default 100 requests per minute per API key

### Security Settings

- **API Keys**: Create multiple keys with different scopes (read-only, read-write, admin)
- **IP Allowlist**: Restrict access to specific IP addresses
- **Row-Level Security**: Define policies per table for fine-grained access control

## Permissions

YourSQL requires the following permissions:

- `INTERNET` - Network communication
- `FOREGROUND_SERVICE` - Keep server running in background
- `POST_NOTIFICATIONS` - Server status notification (Android 13+)
- `READ_EXTERNAL_STORAGE` - File imports
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Prevent Doze mode from stopping the server

## API Reference

### Query Parameters

| Parameter | Description | Example |
|-----------|-------------|---------|
| `select` | Column projection | `select=id,name` |
| `eq` | Equal filter | `id=eq.1` |
| `neq` | Not equal filter | `status=neq.deleted` |
| `lt`, `lte` | Less than (or equal) | `age=lte.18` |
| `gt`, `gte` | Greater than (or equal) | `created_at=gte.2024-01-01` |
| `like` | Pattern matching | `name=like.%John%` |
| `order` | Sorting | `order=created_at.desc` |
| `limit` | Limit results | `limit=10` |
| `offset` | Skip results | `offset=20` |

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (successful DELETE) |
| 400 | Bad Request |
| 401 | Unauthorized (invalid API key) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Not Found |
| 413 | Payload Too Large (storage quota exceeded) |
| 500 | Internal Server Error |

## Troubleshooting

### Server won't start
- Check if port is already in use
- Verify battery optimization is disabled for YourSQL
- Ensure Wi-Fi is connected

### Can't connect from other devices
- Verify devices are on the same network
- Check firewall settings
- Try using the device's IP address instead of hostname

### Storage quota exceeded
- Delete unused files or buckets
- Increase quota in Settings

## License

GNU GPLV3 

## Contributing

Contributions are welcome! Please read CONTRIBUTING.md for guidelines.
