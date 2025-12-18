# Stepps - Daily Step Tracker & Fitness Companion

![Android](https://img.shields.io/badge/Android-Studio-green.svg)
![Min SDK](https://img.shields.io/badge/minSDK-24-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-orange.svg)
![Status](https://img.shields.io/badge/Status-Completed-success.svg)

A simple, motivational fitness app that helps users stay active by tracking their daily steps, walking time, distance, and calories burned with beautiful visualizations and achievement systems.

## ✨ Features

### Core Functionality
- **Multi-User Authentication**: Secure user registration and login with password encryption
- **Live Step Counter**: Real-time step tracking using device sensors
- **Progress Visualization**: Circular progress ring showing daily goal completion
- **Activity Metrics**: Displays steps, distance, calories, and active time
- **Streak Tracking**: Motivates users with consecutive active days
- **Achievement System**: Unlock badges for reaching milestones
- **Data Isolation**: Each user's data is completely separate and secure

### Screens & Layouts
- **Login/Register**: Secure local authentication with guest mode option
- **Dashboard**: Main screen with live stats and progress visualization
- **History**: Calendar view with daily, weekly, and monthly statistics
- **Goals & Achievements**: Set personal goals and unlock badges
- **Settings**: Customize profile, theme, notifications, and units

### Sensors Used
- **Accelerometer**: Motion detection and step counting with false movement filtering
- **Gyroscope**: Improved accuracy through phone orientation recognition
- **GPS**: Route tracking and precise outdoor distance measurement

### Data Storage
- **SQLite Database**: Multi-user support with secure authentication
  - User table with hashed passwords (SHA-256)
  - Daily steps table linked to user accounts
  - Achievements tracking per user
  - Foreign key relationships for data integrity
- **Shared Preferences**: Saves app settings and user preferences
- **Local-First**: All data stored locally for privacy

## ⚙️ Setup Instructions

### Prerequisites
- Android Studio (Arctic Fox or newer)
- Android SDK 24 or higher
- Physical Android device recommended (for accurate sensor testing)

### Installation
1. **Clone or Download** the project files
2. **Open in Android Studio**:
   File → Open → Select the project folder
3. **Sync Gradle**:
   - Android Studio should automatically sync
   - If not: `File → Sync Project with Gradle Files`
4. **Add Required Resources**: Create the following drawable resources in `res/drawable/`:
   - `ic_home.xml` (Home icon)
   - `ic_history.xml` (History icon)
   - `ic_trophy.xml` (Trophy icon)
   - `ic_settings.xml` (Settings icon)
   - `ic_notification.xml` (Notification icon)
   - `ic_badge_locked.xml` (Locked badge)
   - `ic_badge_unlocked.xml` (Unlocked badge)
   - `circular_progress.xml` (Circular progress drawable)

### Grant Permissions (on first run):
- Activity Recognition
- Location Access (for GPS tracking)
- Notifications

## 🚀 Running the App

1. Connect Device or start an emulator
2. Run: Click the green play button in Android Studio
3. Register: Create a new account with email and password
4. Login: Sign in with your credentials or continue as guest
5. Start Tracking: Click "Start Tracking" on the dashboard

## 🔧 Key Features Implementation

### Step Counting Algorithm
The app uses a dual approach:
- **Hardware Step Counter (preferred)**: Uses TYPE_STEP_COUNTER sensor
- **Accelerometer Fallback**: Custom algorithm detecting movement patterns

### Multi-User Database
- **User Authentication**: Passwords hashed with SHA-256 for security
- **Data Isolation**: Each user's steps, achievements, and settings are separate
- **Persistent Storage**: Data survives app restarts and user sessions
- **Guest Mode**: Temporary tracking without account creation

### Data Persistence
- Daily steps automatically saved to SQLite database
- Progress tracked across app restarts
- User-specific data retrieval on login
- Export functionality for data portability

### Achievements System
Unlock badges for:
- 🥇 First 1K Steps - First milestone
- 🔥 Week Streak - 7 consecutive active days
- 🏆 10K Club - 10,000 steps in one day

### Dark Mode Support
- System-wide theme switching
- Persistent preference storage
- Material Design compliant

## 📊 Milestone Requirements

### Milestone 1 (10%) ✅
**Three distinctive functional layouts:**
- Login/Register
- Dashboard with live stats
- History/Goals/Settings

**User interaction:**
- Login/registration forms
- Start/Stop tracking button
- Goal settings

**Intents:**
- Navigation between activities
- Service communication

### Milestone 2 (15%) ✅
**Sensor data collection:**
- Accelerometer for step counting
- Gyroscope for accuracy improvement
- GPS for route tracking

**Dynamic and practical use:**
- Real-time step counter updates
- Progress visualization
- Historical data display

### Milestone 3 (25%) ✅
**Database/File Storage:**
- SQLite database with 3 tables (users, daily_steps, achievements)
- User authentication with password hashing
- Foreign key relationships
- User-specific data queries

**Persistent Storage:**
- Daily steps saved per user
- Data survives app restarts
- Login sessions maintained
- SharedPreferences for settings

**Mobile Optimization:**
- Background service for continuous tracking
- Efficient sensor sampling
- Battery-friendly GPS intervals
- Foreground service notification

## 🧪 Testing

### Recommended Testing
- **Step Counter**: Walk around with device
- **Multi-User**: Create 2 accounts, verify data separation
- **Login/Logout**: Test password verification and session persistence
- **Guest Mode**: Ensure temporary tracking works
- **Streak System**: Change device date to test consecutive days
- **Dark Mode**: Toggle in settings
- **Database**: Add multiple days of data
- **Achievements**: Reach different milestones

### Known Limitations
- Emulator testing limited (no real sensor data)
- GPS requires outdoor testing
- Battery optimization may affect background trackin

## 📝 License
Educational project 

## 👨‍💻 Author
Aden Ibrahim Salat  
Student ID: 3107940  
BSC-MD Mobile Development Course

**Note**: This is a student project for educational purposes. Always test thoroughly on physical devices for accurate sensor readings.

---
