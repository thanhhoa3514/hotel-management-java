# Keycloak Configuration Guide - Complete CLI Setup

## 📚 Table of Contents
- [Installation](#installation)
- [CLI Setup](#cli-setup)
- [Realm Configuration](#realm-configuration)
- [Social Login Setup](#social-login-setup)
- [Redirect URLs](#redirect-urls)
- [Users & Roles](#users--roles)
- [Integration](#integration)

---

## 🚀 Installation

### Via Docker (Recommended)
```yaml
# docker-compose.yml (already configured)
keycloak:
  image: quay.io/keycloak/keycloak:23.0.0
  container_name: hotel_keycloak
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin123
  ports:
    - "8180:8080"
  command: start-dev
```

```bash
# Start Keycloak
docker-compose up -d keycloak

# Access Admin Console
http://localhost:8180
Login: admin / admin123
```

---

## 💻 CLI Setup

### 1. Login to Keycloak Admin CLI
```bash
# Enter container
docker exec -it hotel_keycloak bash

# Login
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin123

# ✅ Logged in successfully
```

### 2. Alternative: Use Admin CLI from Host
```bash
# Set alias for convenience
alias kcadm="docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh"

# Test
kcadm config credentials --server http://localhost:8080 \
  --realm master --user admin --password admin123
```

---

## 🏰 Realm Configuration

### Create Realm via CLI

```bash
# Create realm
kcadm create realms \
  -s realm=hotel-realm \
  -s enabled=true \
  -s displayName="Hotel Management System" \
  -s registrationAllowed=true \
  -s resetPasswordAllowed=true \
  -s rememberMe=true

# ✅ Created new realm with id 'hotel-realm'
```

### Configure Realm Settings
```bash
# Update realm settings
kcadm update realms/hotel-realm \
  -s loginTheme=keycloak \
  -s accountTheme=keycloak \
  -s adminTheme=keycloak \
  -s emailTheme=keycloak \
  -s sslRequired=NONE \
  -s accessCodeLifespan=60 \
  -s accessTokenLifespan=300

# Enable email verification
kcadm update realms/hotel-realm \
  -s verifyEmail=true \
  -s loginWithEmailAllowed=true
```

### Export Realm Configuration
```bash
# Export to JSON
kcadm get realms/hotel-realm > hotel-realm-config.json

# Import realm from JSON
kcadm create realms -f hotel-realm-config.json
```

---

## 👤 Create Client Application

### 1. Create Client
```bash
# Create public client for frontend
kcadm create clients -r hotel-realm \
  -s clientId=hotel-app \
  -s enabled=true \
  -s publicClient=true \
  -s directAccessGrantsEnabled=true \
  -s standardFlowEnabled=true \
  -s implicitFlowEnabled=false \
  -s serviceAccountsEnabled=false \
  -s 'redirectUris=["http://localhost:5173/*"]' \
  -s 'webOrigins=["http://localhost:5173"]' \
  -s protocol=openid-connect

# ✅ Created new client with id '...'
```

### 2. Configure Client Settings
```bash
# Get client UUID
CLIENT_ID=$(kcadm get clients -r hotel-realm --fields id,clientId \
  | jq -r '.[] | select(.clientId=="hotel-app") | .id')

# Update redirect URIs
kcadm update clients/$CLIENT_ID -r hotel-realm \
  -s 'redirectUris=["http://localhost:5173/*","http://localhost:3000/*"]' \
  -s 'webOrigins=["http://localhost:5173","http://localhost:3000"]' \
  -s baseUrl="http://localhost:5173"

# Enable CORS
kcadm update clients/$CLIENT_ID -r hotel-realm \
  -s 'attributes={
    "access.token.lifespan": "300",
    "post.logout.redirect.uris": "+",
    "pkce.code.challenge.method": "S256"
  }'
```

---

## 🔐 Social Login Setup

### Google OAuth Configuration

#### 1. Get Google Credentials
```
1. Go to: https://console.cloud.google.com/
2. Create New Project: "Hotel Management"
3. Enable Google+ API
4. Create OAuth 2.0 Credentials
5. Add Authorized Redirect URI:
   http://localhost:8180/realms/hotel-realm/broker/google/endpoint
```

#### 2. Configure in Keycloak (CLI)
```bash
# Create Google Identity Provider
kcadm create identity-provider/instances -r hotel-realm \
  -s alias=google \
  -s providerId=google \
  -s enabled=true \
  -s 'config.useJwksUrl="true"' \
  -s 'config.clientId="YOUR_GOOGLE_CLIENT_ID"' \
  -s 'config.clientSecret="YOUR_GOOGLE_CLIENT_SECRET"' \
  -s 'config.defaultScope="openid profile email"' \
  -s trustEmail=true \
  -s storeToken=false \
  -s addReadTokenRoleOnCreate=false

# ✅ Created new identity-provider instance with id 'google'
```

#### 3. Configure Mappers
```bash
# Email mapper
kcadm create identity-provider/instances/google/mappers -r hotel-realm \
  -s name=email \
  -s identityProviderAlias=google \
  -s identityProviderMapper=oidc-user-attribute-idp-mapper \
  -s 'config.claim=email' \
  -s 'config.user.attribute=email'

# Full name mapper
kcadm create identity-provider/instances/google/mappers -r hotel-realm \
  -s name=fullName \
  -s identityProviderAlias=google \
  -s identityProviderMapper=oidc-user-attribute-idp-mapper \
  -s 'config.claim=name' \
  -s 'config.user.attribute=fullName'
```

---

### Facebook OAuth Configuration

#### 1. Get Facebook Credentials
```
1. Go to: https://developers.facebook.com/
2. Create App: "Hotel Management"
3. Add Facebook Login product
4. Valid OAuth Redirect URIs:
   http://localhost:8180/realms/hotel-realm/broker/facebook/endpoint
```

#### 2. Configure in Keycloak (CLI)
```bash
# Create Facebook Identity Provider
kcadm create identity-provider/instances -r hotel-realm \
  -s alias=facebook \
  -s providerId=facebook \
  -s enabled=true \
  -s trustEmail=true \
  -s 'config.clientId="YOUR_FACEBOOK_APP_ID"' \
  -s 'config.clientSecret="YOUR_FACEBOOK_APP_SECRET"' \
  -s 'config.defaultScope="email public_profile"'
```

---

## 🔗 Redirect URLs Configuration

### Frontend Redirect URIs

```bash
# Development URLs
REDIRECT_URIS='[
  "http://localhost:5173/*",
  "http://localhost:5173/auth/callback",
  "http://localhost:3000/*"
]'

# Production URLs
REDIRECT_URIS='[
  "https://yourdomain.com/*",
  "https://yourdomain.com/auth/callback",
  "https://app.yourdomain.com/*"
]'

# Update client
kcadm update clients/$CLIENT_ID -r hotel-realm \
  -s "redirectUris=$REDIRECT_URIS"
```

### Post-Logout Redirect URIs
```bash
kcadm update clients/$CLIENT_ID -r hotel-realm \
  -s 'attributes.post.logout.redirect.uris=http://localhost:5173/*'
```

### Web Origins (CORS)
```bash
WEB_ORIGINS='[
  "http://localhost:5173",
  "http://localhost:3000",
  "https://yourdomain.com"
]'

kcadm update clients/$CLIENT_ID -r hotel-realm \
  -s "webOrigins=$WEB_ORIGINS"
```

---

## 👥 Users & Roles Management

### Create Roles
```bash
# Create realm roles
kcadm create roles -r hotel-realm \
  -s name=ADMIN \
  -s description="Administrator role"

kcadm create roles -r hotel-realm \
  -s name=STAFF \
  -s description="Staff role"

kcadm create roles -r hotel-realm \
  -s name=GUEST \
  -s description="Guest role"
```

### Create Users
```bash
# Create admin user
kcadm create users -r hotel-realm \
  -s username=admin@hotel.com \
  -s email=admin@hotel.com \
  -s firstName=Admin \
  -s lastName=User \
  -s enabled=true \
  -s emailVerified=true

# Get user ID
USER_ID=$(kcadm get users -r hotel-realm -q username=admin@hotel.com \
  | jq -r '.[0].id')

# Set password
kcadm set-password -r hotel-realm \
  --username admin@hotel.com \
  --new-password admin123 \
  --temporary false

# Assign role
kcadm add-roles -r hotel-realm \
  --uid $USER_ID \
  --rolename ADMIN
```

### Batch User Creation
```bash
# users.json
cat > users.json << 'EOF'
[
  {
    "username": "staff1@hotel.com",
    "email": "staff1@hotel.com",
    "firstName": "Staff",
    "lastName": "One",
    "enabled": true,
    "credentials": [{
      "type": "password",
      "value": "password123",
      "temporary": false
    }],
    "realmRoles": ["STAFF"]
  }
]
EOF

# Import users
kcadm create users -r hotel-realm -f users.json
```

---

## 📧 Email Configuration

### SMTP Settings
```bash
# Configure email server
kcadm update realms/hotel-realm \
  -s 'smtpServer.host=smtp.gmail.com' \
  -s 'smtpServer.port=587' \
  -s 'smtpServer.from=noreply@hotel.com' \
  -s 'smtpServer.fromDisplayName=Hotel Management' \
  -s 'smtpServer.auth=true' \
  -s 'smtpServer.starttls=true' \
  -s 'smtpServer.user=your-email@gmail.com' \
  -s 'smtpServer.password=your-app-password'
```

### Test Email
```bash
# Trigger password reset email (tests SMTP)
kcadm create users/$USER_ID/execute-actions-email -r hotel-realm \
  -s actions='["UPDATE_PASSWORD"]'
```

---

## 🎨 Custom Themes & Branding

### Upload Custom Theme
```bash
# Copy theme to Keycloak
docker cp ./custom-theme hotel_keycloak:/opt/keycloak/themes/

# Set theme for realm
kcadm update realms/hotel-realm \
  -s loginTheme=custom-theme \
  -s accountTheme=custom-theme \
  -s emailTheme=custom-theme
```

---

## 🔍 Debugging & Monitoring

### Get Realm Info
```bash
# Full realm configuration
kcadm get realms/hotel-realm

# Specific fields
kcadm get realms/hotel-realm --fields id,realm,enabled,loginTheme
```

### List Users
```bash
# All users
kcadm get users -r hotel-realm

# Search by email
kcadm get users -r hotel-realm -q email=admin@hotel.com

# With roles
kcadm get users -r hotel-realm -q username=admin@hotel.com \
  --fields id,username,email,enabled,realmRoles
```

### List Clients
```bash
# All clients
kcadm get clients -r hotel-realm

# Specific client
kcadm get clients/$CLIENT_ID -r hotel-realm
```

### Check Identity Providers
```bash
# List all providers
kcadm get identity-provider/instances -r hotel-realm

# Specific provider
kcadm get identity-provider/instances/google -r hotel-realm
```

---

## 🚀 Complete Setup Script

```bash
#!/bin/bash
# setup-keycloak.sh

# Login
docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin123

# Create realm
docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh create realms \
  -s realm=hotel-realm -s enabled=true -s displayName="Hotel Management"

# Create client
docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh create clients -r hotel-realm \
  -s clientId=hotel-app \
  -s publicClient=true \
  -s 'redirectUris=["http://localhost:5173/*"]' \
  -s 'webOrigins=["http://localhost:5173"]'

# Create roles
for role in ADMIN STAFF GUEST; do
  docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh create roles -r hotel-realm \
    -s name=$role
done

# Create admin user
docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh create users -r hotel-realm \
  -s username=admin@hotel.com \
  -s email=admin@hotel.com \
  -s enabled=true \
  -s emailVerified=true

# Set password
docker exec -it hotel_keycloak /opt/keycloak/bin/kcadm.sh set-password -r hotel-realm \
  --username admin@hotel.com \
  --new-password admin123

echo "✅ Keycloak setup complete!"
```

---

## 📊 Complete Configuration Summary

### Realm Settings
| Setting | Value |
|---------|-------|
| Realm Name | `hotel-realm` |
| Display Name | Hotel Management System |
| Registration | Enabled |
| Email Verification | Enabled |
| SSL Required | None (dev) / External (prod) |

### Client Settings
| Setting | Value |
|---------|-------|
| Client ID | `hotel-app` |
| Client Type | Public |
| Root URL | `http://localhost:5173` |
| Redirect URIs | `http://localhost:5173/*` |
| Web Origins | `http://localhost:5173` |

### Social Providers
| Provider | Redirect URI |
|----------|--------------|
| Google | `http://localhost:8180/realms/hotel-realm/broker/google/endpoint` |
| Facebook | `http://localhost:8180/realms/hotel-realm/broker/facebook/endpoint` |

---

**✨ Keycloak CLI Configuration Complete!**
