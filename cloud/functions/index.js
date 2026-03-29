/**
 * Import function triggers from v2 API where possible.
 */
const { onCall } = require('firebase-functions/v2/https');
const { HttpsError } = require('firebase-functions/v2/https');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

// Helper: round to nearest 15 minutes (milliseconds)
function roundToQuarter(ms) {
  const bucket = 15 * 60 * 1000;
  return Math.round(ms / bucket) * bucket;
}

/**
 * Verifies authentication - tries context.auth first, then falls back to payload.userId or payload.studentId
 */
async function verifyAuth(context, payload) {
  // Primary auth: context.auth (from ID token)
  if (context.auth && context.auth.uid) {
    logger.info('Auth: Using context.auth.uid', { uid: context.auth.uid });
    return context.auth.uid;
  }

  logger.warn('Auth: context.auth not available, trying fallback');

  // The payload is nested - extract it
  // In Firebase Functions v2 onCall, the data parameter might have the payload nested in .data
  const actualPayload = payload?.data || payload;

  logger.info('Auth: Checking payload', {
    hasPayload: !!actualPayload,
    payloadKeys: actualPayload ? Object.keys(actualPayload) : []
  });

  // Make sure payload exists and is an object
  if (!actualPayload || typeof actualPayload !== 'object') {
    logger.error('Auth: payload is not a valid object', { payloadType: typeof actualPayload });
    throw new HttpsError('unauthenticated', 'Authentication required. Please log in.');
  }

  // Fallback 1: userId from payload
  if (actualPayload.userId) {
    logger.info('Auth: Using userId fallback', { userId: actualPayload.userId });
    return actualPayload.userId;
  }

  // Fallback 2: studentId from payload
  if (actualPayload.studentId) {
    logger.warn('Auth: Using studentId final fallback', { studentId: actualPayload.studentId });
    return actualPayload.studentId;
  }

  // No auth found
  logger.error('Auth: Failed - no authentication found', {
    payloadKeys: actualPayload ? Object.keys(actualPayload) : [],
    userId: actualPayload?.userId,
    studentId: actualPayload?.studentId
  });

  throw new HttpsError('unauthenticated', 'Authentication required. Please log in.');
}

// Callable function to create a session request.
exports.createSessionRequest = onCall(
  { region: 'us-central1', enforceAppCheck: false },
  async (data, context) => {
    try {
      logger.info('createSessionRequest: Starting', {
        hasAuth: !!context.auth,
        hasData: !!data
      });

      // Extract the actual payload from the nested structure
      const payload = data?.data || data;

      // Verify authentication
      const callerUid = await verifyAuth(context, data);
      logger.info('createSessionRequest: Auth verified', { callerUid });

      const studentId = payload.studentId;
      const tutorId = payload.tutorId || payload.teacherId;
      const domain = payload.domain;
      const requestedTimeMillis = Number(payload.requestedTimeMillis || payload.requestedTime);
      const durationMinutes = Number(payload.durationMinutes || 60);
      const message = payload.message || '';

      logger.info('createSessionRequest: Extracted payload', {
        studentId,
        tutorId,
        domain,
        requestedTimeMillis,
        durationMinutes
      });

      // Validate required fields
      if (!studentId || !tutorId || !domain || !requestedTimeMillis || !durationMinutes) {
        logger.error('Missing required fields', {
          studentId: !!studentId,
          tutorId: !!tutorId,
          domain: !!domain,
          requestedTimeMillis: !!requestedTimeMillis,
          durationMinutes: !!durationMinutes
        });
        throw new HttpsError('invalid-argument', 'Missing required fields');
      }

      // User can only create requests for themselves
      if (callerUid !== studentId) {
        throw new HttpsError('permission-denied', 'You may only create requests for your own account');
      }

      // Normalize time to nearest 15 minutes
      const now = Date.now();
      const requestedRounded = roundToQuarter(requestedTimeMillis);

      // Validate time is in future and within 7 days
      const localNow = new Date();
      const todayStart = new Date(localNow.getFullYear(), localNow.getMonth(), localNow.getDate()).getTime();
      const weekEnd = todayStart + 7 * 24 * 60 * 60 * 1000;

      if (requestedRounded < todayStart || requestedRounded > weekEnd) {
        throw new HttpsError('failed-precondition', 'Requested time must be within today and the next 7 days');
      }

      if (requestedRounded <= now - 1000) {
        throw new HttpsError('failed-precondition', 'Requested time must be in the future');
      }

      // Create session document
      const sessionDoc = {
        studentId,
        teacherId: tutorId,
        domain,
        requestedTime: admin.firestore.Timestamp.fromMillis(requestedRounded),
        durationMinutes,
        message,
        status: 'requested',
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      };

      const docRef = await db.collection('sessions').add(sessionDoc);
      logger.info('✓ Session created successfully', { sessionId: docRef.id, studentId, tutorId });

      // Try to send notification to tutor
      try {
        const tutorDoc = await db.collection('users').doc(tutorId).get();
        if (tutorDoc.exists) {
          const tutorData = tutorDoc.data() || {};
          const token = tutorData.fcmToken || tutorData.pushToken;
          if (token) {
            await admin.messaging().sendToDevice(token, {
              notification: {
                title: 'New session request',
                body: `You have a new session request from ${studentId}`
              },
              data: { type: 'session_request', sessionId: docRef.id }
            }).catch(err => logger.warn('FCM failed', err));
          }
        }
      } catch (e) {
        logger.warn('Failed to notify tutor:', e);
      }

      return { sessionId: docRef.id };
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      logger.error('createSessionRequest error:', err);
      throw new HttpsError('unknown', err.message || 'Failed to create session');
    }
  }
);

// Confirm session request atomically
exports.confirmSessionRequest = onCall(
  { region: 'us-central1', enforceAppCheck: false },
  async (data, context) => {
    try {
      // Extract the actual payload from the nested structure
      const payload = data?.data || data;

      logger.info('confirmSessionRequest: Starting', {
        hasData: !!data,
        hasPayload: !!payload,
        payloadKeys: payload ? Object.keys(payload) : []
      });

      // Verify authentication
      const userId = await verifyAuth(context, data);
      logger.info('confirmSessionRequest: Auth verified', { userId });

      const requestId = payload.requestId;
      logger.info('confirmSessionRequest: Extracted requestId', { requestId });

      if (!requestId) {
        logger.error('confirmSessionRequest: Missing requestId', {
          payloadKeys: payload ? Object.keys(payload) : [],
          payload: payload
        });
        throw new HttpsError('invalid-argument', 'Missing requestId');
      }

      // Run transaction to ensure atomicity
      const result = await db.runTransaction(async (tx) => {
        const reqRef = db.collection('sessions').doc(requestId);
        const reqSnap = await tx.get(reqRef);

        if (!reqSnap.exists) {
          throw new HttpsError('not-found', 'Session request not found');
        }

        const req = reqSnap.data();
        const teacherId = req.teacherId;
        const studentId = req.studentId;
        const durationMinutes = req.durationMinutes || 60;

        // Allow confirmation by:
        // 1. Teacher (for initial requests or accepting student's reschedule proposal)
        // 2. Student (for accepting teacher's reschedule proposal)
        const isTeacher = userId === teacherId;
        const isStudent = userId === studentId;

        if (!isTeacher && !isStudent) {
          throw new HttpsError('permission-denied', 'Only the teacher or student involved may confirm');
        }

        // If there's a reschedule proposal, allow the OTHER party to accept it
        // Otherwise, only teacher can confirm initial requests
        if (req.status === 'reschedule_pending' && req.rescheduleProposedBy) {
          // Student proposed reschedule: only teacher can accept
          // Teacher proposed reschedule: only student can accept
          if (req.rescheduleProposedBy === 'student' && !isTeacher) {
            throw new HttpsError('permission-denied', 'Only the teacher may accept student reschedule proposals');
          }
          if (req.rescheduleProposedBy === 'teacher' && !isStudent) {
            throw new HttpsError('permission-denied', 'Only the student may accept teacher reschedule proposals');
          }
        } else {
          // Initial request: only teacher can confirm
          if (!isTeacher) {
            throw new HttpsError('permission-denied', 'Only the teacher may confirm initial requests');
          }
        }

        // Use rescheduleProposedTime if available, otherwise use requestedTime
        const timeToUse = req.rescheduleProposedTime || req.requestedTime;
        if (!timeToUse) {
          throw new HttpsError('failed-precondition', 'No time available for confirmation');
        }

        // Convert time to milliseconds
        const startMs = timeToUse._seconds
          ? timeToUse._seconds * 1000 + Math.floor((timeToUse._nanoseconds || 0) / 1e6)
          : timeToUse.toMillis?.() || Date.parse(timeToUse);
        const endMs = startMs + durationMinutes * 60 * 1000;

        // Check for time conflicts
        const confQuery = await tx.get(db.collection('confirmed_sessions').where('teacherId', '==', teacherId));
        for (const doc of confQuery.docs) {
          const s = doc.get('scheduledTime');
          const sMs = s._seconds ? s._seconds * 1000 + Math.floor((s._nanoseconds || 0) / 1e6) : s.toMillis?.() || Date.parse(s);
          const d = doc.get('durationMinutes') || 60;
          const eMs = sMs + d * 60 * 1000;
          if (!(endMs <= sMs || startMs >= eMs)) {
            throw new HttpsError('failed-precondition', 'Time conflict with existing session');
          }
        }

        // Check calendar blocks
        const blocksQuery = await tx.get(db.collection('teacher_calendar').where('teacherId', '==', teacherId));
        for (const doc of blocksQuery.docs) {
          const s = doc.get('start');
          const e = doc.get('end');
          const sMs = s._seconds ? s._seconds * 1000 + Math.floor((s._nanoseconds || 0) / 1e6) : s.toMillis?.() || Date.parse(s);
          const eMs = e._seconds ? e._seconds * 1000 + Math.floor((e._nanoseconds || 0) / 1e6) : e.toMillis?.() || Date.parse(e);
          if (!(endMs <= sMs || startMs >= eMs)) {
            throw new HttpsError('failed-precondition', 'Time conflict with calendar block');
          }
        }

        // Create confirmed session
        const now = admin.firestore.FieldValue.serverTimestamp();
        const confirmedRef = db.collection('confirmed_sessions').doc();
        tx.set(confirmedRef, {
          requestId,
          teacherId,
          studentId,
          scheduledTime: admin.firestore.Timestamp.fromMillis(startMs),
          durationMinutes,
          status: 'scheduled',
          createdAt: now
        });

        // Add calendar block
        const blockRef = db.collection('teacher_calendar').doc();
        tx.set(blockRef, {
          teacherId,
          start: admin.firestore.Timestamp.fromMillis(startMs),
          end: admin.firestore.Timestamp.fromMillis(endMs),
          source: 'confirmed_session',
          createdAt: now
        });

        // Update session status
        tx.update(reqRef, { status: 'confirmed', updatedAt: now });

        return { confirmedId: confirmedRef.id };
      });

      logger.info('confirmSessionRequest: Success', { confirmedId: result.confirmedId });
      return result;
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      logger.error('confirmSessionRequest error:', err);
      throw new HttpsError('unknown', err.message || 'Failed to confirm session');
    }
  }
);
