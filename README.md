# Stepps - Daily Step Tracker & Fitness Companion

![Android](https://img.shields.io/badge/Android-Studio-green.svg)
![Min SDK](https://img.shields.io/badge/minSDK-24-blue.svg)
![Java](https://img.shields.io/badge/Java-100%25-orange.svg)
![Status](https://img.shields.io/badge/Status-Completed-success.svg)

A simple, motivational fitness app that helps users stay active by tracking their daily steps, walking time, distance, and calories burned with beautiful visualizations and achievement systems.

## 📱 Screenshots

| Login Screen | Dashboard | History View |
|--------------|-----------|--------------|
| *Modern login interface* | *Live stats & progress ring* | *Calendar & statistics* |

## ✨ Features

### Core Functionality
- **Live Step Counter**: Real-time step tracking using device sensors
- **Progress Visualization**: Circular progress ring showing daily goal completion
- **Activity Metrics**: Displays steps, distance, calories, and active time
- **Streak Tracking**: Motivates users with consecutive active days
- **Achievement System**: Unlock badges for reaching milestones

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
- **SQLite Database**: Stores daily steps, achievements, and user profile
- **Shared Preferences**: Saves app settings and user preferences
- **Local-First**: All data stored locally for privacy

## 🏗️ Project Structure
com.stepps.fitness/
├── LoginActivity.java # Login/Registration screen
├── MainActivity.java # Main container with bottom navigation
├── StepCounterService.java # Background service for step counting
│
├── fragments/
│ ├── DashboardFragment.java # Home screen with live stats
│ ├── HistoryFragment.java # Activity history and statistics
│ ├── GoalsFragment.java # Goals and achievements
│ └── SettingsFragment.java # App settings and preferences
│
├── database/
│ └── DatabaseHelper.java # SQLite database management
│
├── adapters/
│ ├── HistoryAdapter.java # RecyclerView adapter for history
│ └── AchievementAdapter.java # RecyclerView adapter for badges
│
└── models/
└── Achievement.java # Achievement data model

text

## ⚙️ Setup Instructions

### Prerequisites
- Android Studio (Arctic Fox or newer)
- Android SDK 24 or higher
- Physical Android device recommended (for accurate sensor testing)

### Installation
1. **Clone or Download** the project files
2. **Open in Android Studio**:
File → Open → Select the project folder

text
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

### Example `circular_progress.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
 <item>
     <shape android:shape="ring"
         android:thicknessRatio="16"
         android:useLevel="false">
         <solid android:color="#E0E0E0"/>
     </shape>
 </item>
 <item>
     <shape android:shape="ring"
         android:thicknessRatio="16"
         android:useLevel="true">
         <solid android:color="@color/primary"/>
     </shape>
 </item>
</layer-list>
Add to build.gradle (Project level):
gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
Grant Permissions (on first run):
Activity Recognition

Location Access (for GPS tracking)

Notifications

🚀 Running the App
Connect Device or start an emulator

Run: Click the green play button in Android Studio

Login: Create an account or continue as guest

Start Tracking: Click "Start Tracking" on the dashboard

🔧 Key Features Implementation
Step Counting Algorithm
The app uses a dual approach:

Hardware Step Counter (preferred): Uses TYPE_STEP_COUNTER sensor

Accelerometer Fallback: Custom algorithm detecting movement patterns

Data Persistence
Daily steps automatically saved to SQLite database

Progress tracked across app restarts

Export functionality for data portability

Achievements System
Unlock badges for:

🥇 First 1K Steps - First milestone

🔥 Week Streak - 7 consecutive active days

🏆 10K Club - 10,000 steps in one day

📈 50K Total Steps - Lifetime achievement

💯 100K Club - 100,000 total steps

🏃 Marathon Walker - Walk 42 km total

📅 Month Master - Active for 30 days

Dark Mode Support
System-wide theme switching

Persistent preference storage

Material Design compliant

📊 Milestone Requirements
Milestone 1 (10%) ✅
Three distinctive functional layouts:

Login/Register

Dashboard with live stats

History/Goals/Settings

User interaction:

Login/registration forms

Start/Stop tracking button

Goal settings

Intents:

Navigation between activities

Service communication

Milestone 2 (15%) ✅
Sensor data collection:

Accelerometer for step counting

Gyroscope for accuracy improvement

GPS for route tracking

Dynamic and practical use:

Real-time step counter updates

Progress visualization

Historical data display

🧪 Testing
Recommended Testing
Step Counter: Walk around with device

Streak System: Change device date to test consecutive days

Dark Mode: Toggle in settings

Database: Add multiple days of data

Achievements: Reach different milestones

Known Limitations
Emulator testing limited (no real sensor data)

GPS requires outdoor testing

Battery optimization may affect background tracking

🚀 Future Enhancements
☁️ Cloud sync and backup

👥 Social features and challenges

🏥 Integration with health platforms

🗺️ Route mapping visualization

🎨 Customizable widgets

📊 Advanced analytics and insights

📝 License
Educational project for BSC-MD Mobile Development course.

👨‍💻 Author
Aden Ibrahim Salat
Student ID: 3107940
BSC-MD Mobile Development Course

Note: This is a student project for educational purposes. Always test thoroughly on physical devices for accurate sensor readings.

