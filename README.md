# TutorHub

<p align="center">
  <img src="assets/logo.png" width="170">
</p>

TutorHub is an Android tutoring platform built using **Kotlin** and **Jetpack Compose** that enables students to discover tutors, schedule learning sessions, participate in live video tutoring, and interact through a role-based learning ecosystem.

---

## Features

- Secure Firebase Authentication
- Student & Teacher role-based workflows
- Tutor discovery
- Live video tutoring using WebRTC
- Session scheduling and management
- Question posting and discussion
- Push notifications
- Student and Teacher dashboards

---

## Tech Stack

| Category | Technologies |
|-----------|--------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM, Repository Pattern |
| Backend | Firebase |
| Database | Cloud Firestore |
| Authentication | Firebase Authentication |
| Notifications | Firebase Cloud Messaging |
| Video Calling | WebRTC |

---

## Project Architecture

```
UI (Jetpack Compose)
        │
        ▼
ViewModels (7)
        │
        ▼
Repositories (6)
        │
        ▼
Firebase Services
 ├── Authentication
 ├── Firestore
 └── Cloud Messaging
        │
        ▼
WebRTC (Live Video Sessions)
```

---

## Project Highlights

- 15+ user-facing screens
- 7 ViewModels
- 6 Repository classes
- Student & Teacher role-based architecture
- Live video tutoring
- Firebase Authentication
- Cloud Firestore integration
- Push Notifications

---

## Folder Structure

```
app
├── data
│   ├── repository
│   └── model
├── ui
│   ├── screens
│   ├── navigation
│   └── theme
├── ViewModel
├── util
└── webrtc
```

---

## Installation

1. Clone the repository

```bash
git clone https://github.com/akash-231203/TutorHub.git
```

2. Open in Android Studio

3. Add your Firebase configuration (`google-services.json`)

4. Build and Run

---

## Future Improvements

- Payment integration
- In-app messaging
- Session recording and feedback

---

## Screenshots

## Screenshots

<p align="center">
  <img src="screenshots/Signup Page.jpg" width="250"/>
  <img src="screenshots/Login Page.jpg" width="250"/>
</p>

<p align="center">
  <img src="screenshots/Student Dashboard.jpg" width="250"/>
  <img src="screenshots/Tutor Dashboard.jpg" width="250"/>
</p>

<p align="center">
  <img src="screenshots/Question Posting Screen.jpg" width="250"/>
  <img src="screenshots/Request Session Page.jpg" width="250"/>
</p>

<p align="center">
  <img src="screenshots/Session Requests (Student).jpg" width="250"/>
  <img src="screenshots/Session Requests (Tutor).jpg" width="250"/>
</p>

<p align="center">
  <img src="screenshots/Domain Discovery.jpg" width="250"/>
  <img src="screenshots/Tutor Discovery.jpg" width="250"/>
</p>

---

## Author

Akash Kumar
