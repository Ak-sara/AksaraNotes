# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Aksara Notes is an Android app written in Kotlin that combines secure note-taking with custom database creation. It's a personal information management system with encryption, biometric authentication, and dynamic form builders.

## Build System & Commands

This is a standard Android project using Gradle with Kotlin DSL:

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean project
./gradlew clean

# Install debug build on connected device
./gradlew installDebug
```

## Dependencies & Technology Stack

- **Database**: MongoDB Realm Kotlin (io.realm.kotlin)
- **Authentication**: AndroidX Biometric API
- **Encryption**: AndroidX Security Crypto
- **Architecture**: MVVM with Repository pattern
- **UI**: ViewBinding, Material Design Components
- **Async**: Kotlin Coroutines
- **Navigation**: AndroidX Navigation Component

## Code Architecture

### Core Structure
- **Application Class**: `AksaraApplication.kt` - Initializes Realm and authentication
- **Main Activity**: `MainActivity.kt` - Handles authentication flow and navigation
- **Database Layer**: Realm entities in `data/database/entities/`
- **Repository Pattern**: Data access through `data/repository/`
- **MVVM**: ViewModels in each UI module

### Key Components

1. **Authentication System**
   - `AuthenticationManager` - Global auth state
   - `BiometricHelper` - Handles biometric/password auth
   - `SessionManager` - Manages session timeout
   - `NoteSecurityHelper` - PIN protection for individual notes

2. **Database Architecture**
   - `RealmDatabase` - Singleton database manager
   - Entities: `Note`, `Dataset`, `Form`, `TableColumn`, `TableSettings`
   - DAOs for each entity type in `data/database/dao/`

3. **UI Modules**
   - **Notes**: Standard note-taking with encryption
   - **Calendar**: Shows date fields from databases  
   - **Database**: Dynamic form builder and data management
   - **Settings**: Security and app configuration

4. **Dynamic Database System**
   - `DatasetTemplates` - Pre-built database templates
   - `DynamicFormBuilder` - Runtime form generation
   - `ColumnBuilderAdapter` - UI for creating table columns
   - Formula fields for calculated values

### Security Model
- Master password encrypts all data
- Biometric authentication for app access
- Individual note PIN protection
- Session-based authentication with timeout
- Local-only storage (no cloud sync)

## Key Files for Understanding

- `AksaraApplication.kt:13` - Database initialization
- `MainActivity.kt:54` - Setup flow entry point
- `MainActivity.kt:199` - Authentication flow
- `RealmDatabase.kt:14` - Database configuration
- `data/templates/DatasetTemplates.kt` - Pre-built database types

## Development Notes

- Minimum Android API 31 (Android 12)
- Uses ViewBinding (enabled in `app/build.gradle.kts:41`)
- Realm schema version currently at 1
- Authentication has 3-attempt limit with app termination
- All database operations go through Repository pattern
- Calendar integration shows date fields from custom databases