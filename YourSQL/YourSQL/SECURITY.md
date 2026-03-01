# Security Policy

## Security Model

YourSQL is designed to provide a secure, self-hosted database server on Android devices. This document outlines the security architecture and provides recommendations for safe usage.

## Authentication

### API Key Authentication

YourSQL uses API keys as the primary authentication mechanism:

- **Anonymous Key**: Generated automatically on first launch with 256 bits of entropy
- **Custom Keys**: Users can create additional keys with configurable scopes:
  - `read-only`: SELECT operations only
  - `read-write`: SELECT, INSERT, UPDATE, DELETE
  - `admin`: Full access including schema changes

API keys must be provided in either:
- `apikey` header: `apikey: your-api-key`
- `Authorization` header: `Authorization: Bearer your-api-key`

### JWT Authentication

For user-based authentication, YourSQL supports JWT tokens:

- Tokens are signed with HMAC-SHA256
- Access tokens expire after 1 hour
- Refresh tokens expire after 7 days
- Tokens include user ID, email, and role claims

## Authorization

### Row-Level Security (RLS)

YourSQL implements Row-Level Security policies that can be defined per table:

```sql
-- Example: Allow anonymous users to only see public posts
CREATE POLICY posts_anon_policy ON posts
  FOR SELECT
  USING (is_public = 1);
```

Policies are enforced at the query execution layer before any SQL is run.

### IP Allowlisting

Users can define a list of allowed IP addresses or CIDR ranges:

- If the allowlist is empty, all IPs are allowed
- If the allowlist is non-empty, only listed IPs can access the API
- Requests from non-allowed IPs receive a 403 Forbidden response

## Rate Limiting

Each API key has a configurable rate limit (default: 100 requests per minute):

- Implemented using an in-memory token bucket algorithm
- Limits are per API key, not per IP
- Exceeded requests receive a 429 Too Many Requests response

## Data Storage Security

### Database Files

- User databases are stored in the app's private directory: `/data/data/com.fiozxr.yoursql/databases/`
- Only the YourSQL app can access these files (Android sandboxing)
- No encryption at rest is provided (rely on Android's full-disk encryption)

### Sensitive Data

The following sensitive data is stored securely:

- **API Keys**: Stored in Room database (private app storage)
- **Password Hashes**: SHA-256 hashed with salt
- **JWT Secret**: Generated on first launch, stored in memory only
- **HTTPS Certificate**: Self-signed certificate stored in app private storage

### Backup Exclusions

The following are excluded from Android backups:
- Database files
- API keys
- Certificates
- User credentials

## Network Security

### HTTPS/TLS

YourSQL supports HTTPS using self-signed certificates:

- Certificate is generated on first launch using Bouncy Castle
- 2048-bit RSA key pair
- SHA-256 signature algorithm
- Certificate valid for 1 year

**Important**: Clients will need to import the certificate or disable certificate validation.

### HTTP Security Headers

The server includes the following security headers:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`

## Remote Access Security

### Cloudflare Tunnel

When using Cloudflare Tunnel:
- Tunnel token is stored in encrypted preferences
- HTTPS is terminated at Cloudflare's edge
- Consider this a public exposure - ensure strong authentication

### ngrok

When using ngrok:
- ngrok auth token is stored in encrypted preferences
- Random subdomain is generated for each session
- HTTPS is provided by ngrok

## Security Recommendations

### For Production Use

1. **Enable HTTPS**: Always use HTTPS when exposing the server to any network
2. **Use Strong API Keys**: Rotate keys regularly, use read-only keys where possible
3. **Enable IP Allowlisting**: Restrict access to known IP addresses
4. **Set Rate Limits**: Adjust per-key rate limits based on use case
5. **Configure RLS Policies**: Implement least-privilege access at the row level
6. **Disable When Not Needed**: Stop the server when not actively using it
7. **Keep Updated**: Update YourSQL regularly for security patches

### For Public Exposure

If exposing YourSQL to the internet:

1. **Use Cloudflare Tunnel or ngrok**: Don't expose your device IP directly
2. **Enable All Security Features**: API keys, IP allowlisting, rate limiting, RLS
3. **Monitor Logs**: Regularly check API logs for suspicious activity
4. **Use Strong Passwords**: For JWT authentication, enforce strong passwords
5. **Consider VPN**: Use a VPN for additional network-level security

### For Development/Testing

1. **Use Local Network Only**: Keep the server on your local Wi-Fi
2. **Use HTTP**: HTTPS certificate validation can be disabled for local testing
3. **Anonymous Key**: The auto-generated anonymous key is sufficient for local use

## Known Limitations

1. **No Encryption at Rest**: Database files are not encrypted (rely on Android FDE)
2. **Self-Signed Certificates**: Clients must handle certificate validation
3. **In-Memory Rate Limiting**: Rate limits reset on server restart
4. **No Audit Logging**: Detailed audit logs are not implemented
5. **No Automatic Security Updates**: Users must manually update the app

## Reporting Security Issues

If you discover a security vulnerability in YourSQL, please:

1. **Do not open a public issue**
2. Email security details to: security@fiozxr.com
3. Include steps to reproduce the vulnerability
4. Allow reasonable time for response before public disclosure

## Security Checklist

Before exposing YourSQL to any network:

- [ ] HTTPS is enabled
- [ ] API key authentication is enabled
- [ ] Rate limiting is configured
- [ ] IP allowlist is configured (if applicable)
- [ ] RLS policies are defined for sensitive tables
- [ ] Default/weak API keys have been revoked
- [ ] Battery optimization is disabled for YourSQL
- [ ] You understand the risks of exposing your device

## Disclaimer

YourSQL is provided as-is without warranty. Running a server on your mobile device carries inherent security risks. The developers are not responsible for any data loss, unauthorized access, or other security incidents resulting from the use of this software.
