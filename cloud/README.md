# Firebase Cloud Functions and Firestore Rules for TutorHub

This folder contains server-side logic and security rules to support atomic session request creation/confirmation and optional FCM notifications.

Contents:
- functions/: Firebase Cloud Functions (Node.js)
- firestore.rules: Firestore security rules

Quick start (Windows cmd.exe):
1. Install Firebase CLI if needed:
   npm install -g firebase-tools
2. Login and set project:
   firebase login
   firebase use <your-project-id>
3. Deploy security rules:
   firebase deploy --only firestore:rules
4. Install Cloud Functions deps:
   cd functions
   npm install
5. Configure environment variables for FCM token lookup (optional):
   firebase functions:config:set app.fcm_topic_prefix="tutorhub_" app.support_email="support@yourdomain.com"
6. Deploy functions:
   firebase deploy --only functions

Client usage:
- Call callable function createSessionRequest via FirebaseFunctions.getInstance().getHttpsCallable("createSessionRequest").
- Payload example:
  {
    "studentId": "student_uid",
    "tutorId": "tutor_uid",
    "domain": "math",
    "requestedTimeMillis": 1732800000000, // epoch ms
    "durationMinutes": 60,
    "message": "Hi!"
  }
- The server will normalize time to the nearest 15-minute slot, check conflicts atomically, write the request, and optionally send FCM.

Notes:
- Ensure your Android app has Firebase initialized and user authenticated.
- Update Firestore rules to your exact collection paths if they differ.

