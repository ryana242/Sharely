'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const admin = require('firebase-admin');

const PROJECT_ID = 'sharely-7205c';

process.env.GCLOUD_PROJECT = PROJECT_ID;
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
process.env.FIREBASE_STORAGE_EMULATOR_HOST = '127.0.0.1:9199';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();
const auth = admin.auth();
const storage = admin.storage();

const FUNCTION_BASE_URL =
  `http://127.0.0.1:5001/${PROJECT_ID}` +
  '/australia-southeast1';

// --------------------------------------------------
// Helper: Call Cloud Function without authentication
// --------------------------------------------------

async function callFunction(bookingId) {
  const response = await fetch(
    `${FUNCTION_BASE_URL}/sharelyStartFlow`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        data: { bookingId }
      })
    }
  );

  return response.json();
}

// --------------------------------------------------
// Helper: Get Firebase ID Token
// --------------------------------------------------

async function getIdToken(uid) {
  const customToken = await auth.createCustomToken(uid);

  const response = await fetch(
    'http://127.0.0.1:9099/' +
    'identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken' +
    '?key=fake-api-key',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: customToken,
        returnSecureToken: true
      })
    }
  );

  const result = await response.json();

  assert.ok(
    response.ok && result.idToken,
    `Emulator authentication failed: ${JSON.stringify(result)}`
  );

  return result.idToken;
}

// --------------------------------------------------
// Helper: Call authenticated Cloud Function
// --------------------------------------------------

async function callAuthenticatedFunction(
  functionName,
  data,
  idToken
) {
  const response = await fetch(
    `${FUNCTION_BASE_URL}/${functionName}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${idToken}`
      },
      body: JSON.stringify({
        data
      })
    }
  );

  const result = await response.json();

  return { response, result };
}

// --------------------------------------------------
// Helper: Create Android-compatible test data
// --------------------------------------------------

async function createTestBooking() {
  const id = crypto.randomUUID();

  const itemId = `item-${id}`;
  const bookingId = `booking-${id}`;

  const ownerId = `owner-${id}`;
  const borrowerId = `borrower-${id}`;

  const startTime = Date.now() + 24 * 60 * 60 * 1000;
  const endTime = startTime + 24 * 60 * 60 * 1000;

  const itemRef = db.collection('items').doc(itemId);
  const bookingRef = db.collection('bookings').doc(bookingId);

  const flowRef = db
    .collection('sharelyTransactionFlows')
    .doc(bookingId);

  await itemRef.set({
    itemId,
    ownerId,
    itemName: 'Camera',
    description: 'Test camera',
    category: 'Electronics',
    price: 10,
    location: 'Sydney',
    status: 'ACTIVE',
    availabilityStart: startTime,
    availabilityEnd: endTime,
    acceptedReservations: [
      {
        bookingId,
        startTime,
        endTime
      }
    ]
  });

  await bookingRef.set({
    bookingId,
    borrowerId,
    itemId,
    startTime,
    endTime,
    status: 'ACCEPTED'
  });

  return {
    itemId,
    bookingId,
    ownerId,
    borrowerId,
    startTime,
    endTime,
    itemRef,
    bookingRef,
    flowRef
  };
}

// --------------------------------------------------
// Helper: Clean up test data
// --------------------------------------------------

async function cleanupTestData(data) {
  if (!data) {
    return;
  }

  const {
    itemRef,
    bookingRef,
    flowRef,
    ownerId,
    borrowerId
  } = data;

  await Promise.all([
    itemRef.delete(),
    bookingRef.delete(),
    flowRef.delete()
  ]);

  for (const uid of [ownerId, borrowerId]) {
    try {
      await auth.deleteUser(uid);
    } catch (error) {
      if (error.code !== 'auth/user-not-found') {
        throw error;
      }
    }
  }
}

// --------------------------------------------------
// Test 1: Unauthenticated access
// --------------------------------------------------

test(
  'Unauthenticated users cannot start booking flow',
  async () => {
    const result = await callFunction('test-booking-123');

    assert.equal(
      result.error.status,
      'UNAUTHENTICATED'
    );
  }
);

// --------------------------------------------------
// Test 2: Android Booking data structure
// --------------------------------------------------

test(
  'Booking data structure matches Android model',
  async () => {
    const bookingId = `integration-${crypto.randomUUID()}`;

    const booking = {
      bookingId,
      borrowerId: 'bob',
      itemId: 'test-item',
      startTime: Date.now() + 86400000,
      endTime: Date.now() + 172800000,
      status: 'ACCEPTED'
    };

    const bookingRef = db
      .collection('bookings')
      .doc(bookingId);

    try {
      await bookingRef.set(booking);

      const snapshot = await bookingRef.get();

      assert.equal(snapshot.exists, true);

      assert.equal(
        snapshot.data().bookingId,
        bookingId
      );

      assert.equal(
        snapshot.data().borrowerId,
        'bob'
      );

      assert.equal(
        snapshot.data().itemId,
        'test-item'
      );

      assert.equal(
        snapshot.data().status,
        'ACCEPTED'
      );

      assert.equal(
        typeof snapshot.data().startTime,
        'number'
      );

      assert.equal(
        typeof snapshot.data().endTime,
        'number'
      );

    } finally {
      await bookingRef.delete();
    }
  }
);

// --------------------------------------------------
// Test 3: ACCEPTED Booking starts transaction flow
// --------------------------------------------------

test(
  'ACCEPTED booking is compatible with sharelyStartFlow',
  async () => {
    let data;

    try {
      // Step 1: Create test item and booking
      data = await createTestBooking();

      // Step 2: Create borrower
      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      // Step 3: Authenticate borrower
      const idToken = await getIdToken(
        data.borrowerId
      );

      // Step 4: Call Cloud Function
      const { response, result } =
        await callAuthenticatedFunction(
          'sharelyStartFlow',
          {
            bookingId: data.bookingId
          },
          idToken
        );

      assert.ok(
        response.ok && !result.error,
        `Cloud Function failed: ${JSON.stringify(result)}`
      );

      // Step 5: Check transaction document
      const flowSnapshot = await data.flowRef.get();

      assert.equal(
        flowSnapshot.exists,
        true,
        'Transaction flow was not created'
      );

      const flow = flowSnapshot.data();

      assert.equal(
        flow.bookingId,
        data.bookingId
      );

      assert.equal(
        flow.itemId,
        data.itemId
      );

      assert.equal(
        flow.ownerId,
        data.ownerId
      );

      assert.equal(
        flow.borrowerId,
        data.borrowerId
      );

      assert.equal(
        flow.startAt,
        data.startTime
      );

      assert.equal(
        flow.dueAt,
        data.endTime
      );

      assert.equal(
        flow.phase,
        'ACCEPTED'
      );

      assert.equal(
        flow.rentalCents,
        1000
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test 4: Owner schedules pickup
// --------------------------------------------------

test(
  'Owner can schedule pickup for an ACCEPTED booking',
  async () => {
    let data;

    try {
      // Step 1: Create Android-compatible booking
      data = await createTestBooking();

      // Step 2: Create owner and borrower
      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      // Step 3: Authenticate both users
      const borrowerToken = await getIdToken(
        data.borrowerId
      );

      const ownerToken = await getIdToken(
        data.ownerId
      );

      // Step 4: Borrower starts transaction flow
      const startResult =
        await callAuthenticatedFunction(
          'sharelyStartFlow',
          {
            bookingId: data.bookingId
          },
          borrowerToken
        );

      assert.ok(
        startResult.response.ok &&
        !startResult.result.error,
        `Start flow failed: ${JSON.stringify(startResult.result)}`
      );

      assert.equal(
        startResult.result.result.phase,
        'ACCEPTED'
      );

      // Step 5: Owner schedules pickup
      const pickupAt =
        data.startTime + 60 * 60 * 1000;

      const pickupResult =
        await callAuthenticatedFunction(
          'sharelySchedulePickup',
          {
            bookingId: data.bookingId,
            pickupAt
          },
          ownerToken
        );

      assert.ok(
        pickupResult.response.ok &&
        !pickupResult.result.error,
        `Schedule pickup failed: ${JSON.stringify(pickupResult.result)}`
      );

      assert.equal(
        pickupResult.result.result.phase,
        'PICKUP_SCHEDULED'
      );

      // Step 6: Check Firestore transaction flow
      const flowSnapshot = await data.flowRef.get();

      assert.equal(
        flowSnapshot.exists,
        true
      );

      const flow = flowSnapshot.data();

      assert.equal(
        flow.phase,
        'PICKUP_SCHEDULED'
      );

      assert.equal(
        flow.pickupAt,
        pickupAt
      );

      // Step 7: Verify Android Booking status
      const bookingSnapshot =
        await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.exists,
        true
      );

      assert.equal(
        bookingSnapshot.data().status,
        'ACCEPTED'
      );

      // Step 8: Verify booking fields remain unchanged
      assert.equal(
        bookingSnapshot.data().bookingId,
        data.bookingId
      );

      assert.equal(
        bookingSnapshot.data().borrowerId,
        data.borrowerId
      );

      assert.equal(
        bookingSnapshot.data().itemId,
        data.itemId
      );

      assert.equal(
        bookingSnapshot.data().startTime,
        data.startTime
      );

      assert.equal(
        bookingSnapshot.data().endTime,
        data.endTime
      );

// --------------------------------------------------
// Test 5: Owner uploads before-condition photo
// --------------------------------------------------

test(
  'Owner can attach before photo and booking remains compatible',
  async () => {
    let data;
    let bucket;
    let photoPath;

    try {
      // Step 1: Create Android-compatible booking
      data = await createTestBooking();

      // Step 2: Create owner and borrower
      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      // Step 3: Authenticate both users
      const borrowerToken = await getIdToken(
        data.borrowerId
      );

      const ownerToken = await getIdToken(
        data.ownerId
      );

      // Step 4: Start transaction flow
      const startResult =
        await callAuthenticatedFunction(
          'sharelyStartFlow',
          {
            bookingId: data.bookingId
          },
          borrowerToken
        );

      assert.ok(
        startResult.response.ok &&
        !startResult.result.error,
        `Start flow failed: ${JSON.stringify(startResult.result)}`
      );

      // Step 5: Schedule pickup
      const pickupAt =
        data.startTime + 60 * 60 * 1000;

      const pickupResult =
        await callAuthenticatedFunction(
          'sharelySchedulePickup',
          {
            bookingId: data.bookingId,
            pickupAt
          },
          ownerToken
        );

      assert.ok(
        pickupResult.response.ok &&
        !pickupResult.result.error,
        `Schedule pickup failed: ${JSON.stringify(pickupResult.result)}`
      );

      assert.equal(
        pickupResult.result.result.phase,
        'PICKUP_SCHEDULED'
      );

      // Step 6: Upload test image to Storage Emulator
      photoPath =
        `transactionEvidence/${data.bookingId}/${data.ownerId}/before.jpg`;

      bucket = storage.bucket(
        `${PROJECT_ID}.firebasestorage.app`
      );

      const file = bucket.file(photoPath);

      // Minimal valid JPEG image
      const imageBuffer = Buffer.from(
        '/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAGf/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPwA//9k=',
        'base64'
      );

      await file.save(imageBuffer, {
        metadata: {
          contentType: 'image/jpeg'
        }
      });

      const [exists] = await file.exists();

      assert.equal(
        exists,
        true,
        'Test photo was not uploaded'
      );

      // Step 7: Owner attaches before photo
      const photoResult =
        await callAuthenticatedFunction(
          'sharelyAttachBeforePhoto',
          {
            bookingId: data.bookingId,
            storagePath: photoPath
          },
          ownerToken
        );

      assert.ok(
        photoResult.response.ok &&
        !photoResult.result.error,
        `Attach photo failed: ${JSON.stringify(photoResult.result)}`
      );

      assert.equal(
        photoResult.result.result.phase,
        'PICKUP_READY'
      );

      // Step 8: Verify Firestore transaction flow
      const flowSnapshot =
        await data.flowRef.get();

      assert.equal(
        flowSnapshot.exists,
        true
      );

      const flow = flowSnapshot.data();

      assert.equal(
        flow.phase,
        'PICKUP_READY'
      );

      assert.equal(
        flow.beforePhotoPath,
        photoPath
      );

      // Step 9: Verify Android Booking remains unchanged
      const bookingSnapshot =
        await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.exists,
        true
      );

      const booking = bookingSnapshot.data();

      assert.equal(
        booking.status,
        'ACCEPTED'
      );

      assert.equal(
        booking.bookingId,
        data.bookingId
      );

      assert.equal(
        booking.borrowerId,
        data.borrowerId
      );

      assert.equal(
        booking.itemId,
        data.itemId
      );

      assert.equal(
        booking.startTime,
        data.startTime
      );

      assert.equal(
        booking.endTime,
        data.endTime
      );

    } finally {
      // Remove test image from Storage Emulator
      if (bucket && photoPath) {
        try {
          await bucket.file(photoPath).delete({
            ignoreNotFound: true
          });
        } catch (error) {
          console.error(
            'Storage cleanup failed:',
            error.message
          );
        }
      }

      // Remove Firestore and Auth test data
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test 6: Borrower confirms demo rental payment
// --------------------------------------------------

test(
  'Borrower can confirm demo payment without changing Android booking',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const borrowerToken = await getIdToken(data.borrowerId);
      const ownerToken = await getIdToken(data.ownerId);

      // Start transaction
      const start = await callAuthenticatedFunction(
        'sharelyStartFlow',
        { bookingId: data.bookingId },
        borrowerToken
      );

      assert.ok(
        start.response.ok && !start.result.error,
        `Start failed: ${JSON.stringify(start.result)}`
      );

      // Schedule pickup
      const pickup = await callAuthenticatedFunction(
        'sharelySchedulePickup',
        {
          bookingId: data.bookingId,
          pickupAt: data.startTime + 3600000
        },
        ownerToken
      );

      assert.ok(
        pickup.response.ok && !pickup.result.error,
        `Pickup failed: ${JSON.stringify(pickup.result)}`
      );

      // Prepare transaction for payment test.
      // This bypasses photo upload because photo compatibility
      // was already tested separately in Test 5.
      await data.flowRef.update({
        phase: 'PICKUP_READY',
        beforePhotoPath: 'test-photo.jpg'
      });

      // Confirm demo payment
      const payment = await callAuthenticatedFunction(
        'sharelyConfirmDemoRentalPayment',
        { bookingId: data.bookingId },
        borrowerToken
      );

      assert.ok(
        payment.response.ok && !payment.result.error,
        `Payment failed: ${JSON.stringify(payment.result)}`
      );

      assert.equal(payment.result.result.simulated, true);
      assert.equal(payment.result.result.realPaymentProcessed, false);

      // Verify transaction flow
      const flowSnapshot = await data.flowRef.get();
      const flow = flowSnapshot.data();

      assert.equal(flow.phase, 'PICKUP_READY');
      assert.equal(flow.simulatedRentalPaid, true);

      // Verify Android Booking remains unchanged
      const bookingSnapshot = await data.bookingRef.get();
      const booking = bookingSnapshot.data();

      assert.equal(booking.status, 'ACCEPTED');
      assert.equal(booking.bookingId, data.bookingId);
      assert.equal(booking.borrowerId, data.borrowerId);
      assert.equal(booking.itemId, data.itemId);
      assert.equal(booking.startTime, data.startTime);
      assert.equal(booking.endTime, data.endTime);

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test 7: Borrower generates pickup OTP
// --------------------------------------------------

test(
  'Borrower can generate pickup OTP',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const borrowerToken = await getIdToken(data.borrowerId);

      // Prepare a transaction at PICKUP_READY stage.
      // Earlier tests already verified start, scheduling,
      // photo attachment and demo payment independently.
      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,
        phase: 'PICKUP_READY',
        startAt: data.startTime,
        dueAt: data.endTime,
        beforePhotoPath: 'test-before.jpg',
        simulatedRentalPaid: true
      });

      // Borrower requests pickup OTP.
      const otpResult = await callAuthenticatedFunction(
        'sharelyIssuePickupOtp',
        { bookingId: data.bookingId },
        borrowerToken
      );

      assert.ok(
        otpResult.response.ok && !otpResult.result.error,
        `OTP generation failed: ${JSON.stringify(otpResult.result)}`
      );

      const result = otpResult.result.result;

      assert.match(result.code, /^\d{6}$/);
      assert.equal(result.validForSeconds, 300);

      // Verify OTP metadata is saved in Firestore.
      const flowSnapshot = await data.flowRef.get();
      const flow = flowSnapshot.data();

      assert.equal(flow.phase, 'PICKUP_READY');
      assert.ok(flow.pickupOtp);
      assert.equal(typeof flow.pickupOtp.hash, 'string');
      assert.equal(typeof flow.pickupOtp.salt, 'string');
      assert.ok(flow.pickupOtp.expiresAt > Date.now());
      assert.equal(flow.pickupOtp.failedAttempts, 0);

      // Verify Android Booking remains ACCEPTED.
      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'ACCEPTED'
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test 8: Owner verifies pickup OTP
// --------------------------------------------------

test(
  'Owner can verify pickup OTP and activate booking',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      // Create test users.
      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const ownerToken = await getIdToken(data.ownerId);
      const borrowerToken = await getIdToken(data.borrowerId);

      // Prepare PICKUP_READY state.
      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,
        phase: 'PICKUP_READY',
        startAt: data.startTime,
        dueAt: data.endTime,
        beforePhotoPath: 'test-before.jpg',
        simulatedRentalPaid: true
      });

      // Borrower generates pickup OTP.
      const otpResult = await callAuthenticatedFunction(
        'sharelyIssuePickupOtp',
        { bookingId: data.bookingId },
        borrowerToken
      );

      assert.ok(
        otpResult.response.ok && !otpResult.result.error,
        `OTP generation failed: ${JSON.stringify(otpResult.result)}`
      );

      const code = otpResult.result.result.code;

      // Owner verifies pickup OTP.
      const verifyResult = await callAuthenticatedFunction(
        'sharelyVerifyPickupOtp',
        {
          bookingId: data.bookingId,
          code
        },
        ownerToken
      );

      assert.ok(
        verifyResult.response.ok && !verifyResult.result.error,
        `OTP verification failed: ${JSON.stringify(verifyResult.result)}`
      );

      assert.equal(verifyResult.result.result.ok, true);
      assert.equal(verifyResult.result.result.phase, 'ACTIVE');

      // Verify transaction flow.
      const flowSnapshot = await data.flowRef.get();
      const flow = flowSnapshot.data();

      assert.equal(flow.phase, 'ACTIVE');
      assert.equal(flow.pickupOtp, undefined);
      assert.equal(typeof flow.actualPickupAt, 'number');

      // Verify Android Booking status.
      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(bookingSnapshot.data().status, 'ACTIVE');

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test 9: Owner cannot activate booking before start time
// --------------------------------------------------

test(
  'Owner cannot activate booking before start time',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const ownerToken = await getIdToken(data.ownerId);
      const borrowerToken = await getIdToken(data.borrowerId);

      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,
        phase: 'PICKUP_READY',
        startAt: data.startTime,
        dueAt: data.endTime,
        beforePhotoPath: 'test-before.jpg',
        simulatedRentalPaid: true
      });

      const otpResult = await callAuthenticatedFunction(
        'sharelyIssuePickupOtp',
        { bookingId: data.bookingId },
        borrowerToken
      );

      assert.ok(
        otpResult.response.ok && !otpResult.result.error
      );

      const code = otpResult.result.result.code;

      const verifyResult = await callAuthenticatedFunction(
        'sharelyVerifyPickupOtp',
        {
          bookingId: data.bookingId,
          code
        },
        ownerToken
      );

      // Booking starts tomorrow, so activation should be rejected.
      assert.ok(
        !verifyResult.response.ok || verifyResult.result.error,
        'Booking was activated before its start time'
      );

      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'ACCEPTED'
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test 10: Borrower schedules return
// --------------------------------------------------

test(
  'Borrower can schedule return for an ACTIVE booking',
  async () => {
    let data;

    try {
      // Step 1: Create test booking
      data = await createTestBooking();

      // Step 2: Create borrower account
      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const borrowerToken = await getIdToken(
        data.borrowerId
      );

      // Step 3: Set booking status to ACTIVE
      await data.bookingRef.update({
        status: 'ACTIVE'
      });

      // Step 4: Prepare an ACTIVE transaction flow
      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,
        phase: 'ACTIVE',
        startAt: data.startTime,
        dueAt: data.endTime,
        actualPickupAt: Date.now()
      });

      // Step 5: Choose a future return time
      const returnAt = Date.now() + 60 * 60 * 1000;

      // Step 6: Call Cloud Function
      const { response, result } =
        await callAuthenticatedFunction(
          'sharelyScheduleReturn',
          {
            bookingId: data.bookingId,
            returnAt
          },
          borrowerToken
        );

      assert.ok(
        response.ok && !result.error,
        `Schedule return failed: ${JSON.stringify(result)}`
      );

      // Step 7: Verify response
      assert.equal(
        result.result.scheduledReturnAt,
        returnAt
      );

      // Step 8: Verify Firestore transaction flow
      const flowSnapshot = await data.flowRef.get();

      assert.equal(
        flowSnapshot.exists,
        true
      );

      assert.equal(
        flowSnapshot.data().scheduledReturnAt,
        returnAt
      );

      assert.equal(
        flowSnapshot.data().phase,
        'ACTIVE'
      );

      // Step 9: Verify Android Booking remains ACTIVE
      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'ACTIVE'
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test11: Borrower requests return
// --------------------------------------------------

test(
  'Borrower can request return and booking remains ACTIVE',
  async () => {
    let data;

    try {
      // Step 1: Create test booking
      data = await createTestBooking();

      // Step 2: Create borrower
      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      // Step 3: Authenticate borrower
      const borrowerToken = await getIdToken(
        data.borrowerId
      );

      // Step 4: Prepare an ACTIVE transaction flow
      // This represents a booking after successful pickup.
      await data.bookingRef.update({
        status: 'ACTIVE'
      });

      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,

        phase: 'ACTIVE',

        startAt: data.startTime,
        dueAt: data.endTime,

        scheduledReturnAt: Date.now() + 60 * 60 * 1000,

        latePolicy: {
          hourly: 100,
          grace: 15 * 60 * 1000,
          cap: 1000
        },

        simulatedRentalPaid: true,
        simulatedLateFeePaid: false,
        lateFeeCents: 0
      });

      // Step 5: Borrower requests return
      const { response, result } =
        await callAuthenticatedFunction(
          'sharelyRequestReturn',
          {
            bookingId: data.bookingId
          },
          borrowerToken
        );

      assert.ok(
        response.ok && !result.error,
        `Request return failed: ${JSON.stringify(result)}`
      );

      // Step 6: Check returned result
      assert.equal(
        result.result.phase,
        'RETURN_REQUESTED'
      );

      assert.equal(
        result.result.realPaymentProcessed,
        false
      );

      // Step 7: Check Firestore transaction flow
      const flowSnapshot = await data.flowRef.get();

      assert.equal(
        flowSnapshot.exists,
        true
      );

      const flow = flowSnapshot.data();

      assert.equal(
        flow.phase,
        'RETURN_REQUESTED'
      );

      assert.equal(
        typeof flow.actualReturnRequestedAt,
        'number'
      );

      assert.equal(
        typeof flow.lateFeeCents,
        'number'
      );

      assert.ok(
        flow.lateFeeCents >= 0
      );

      // Step 8: Check Android Booking remains ACTIVE
      const bookingSnapshot =
        await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'ACTIVE'
      );

      // Step 9: Verify original booking fields
      assert.equal(
        bookingSnapshot.data().bookingId,
        data.bookingId
      );

      assert.equal(
        bookingSnapshot.data().borrowerId,
        data.borrowerId
      );

      assert.equal(
        bookingSnapshot.data().itemId,
        data.itemId
      );

      assert.equal(
        bookingSnapshot.data().startTime,
        data.startTime
      );

      assert.equal(
        bookingSnapshot.data().endTime,
        data.endTime
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test12: Borrower confirms demo late fee
// --------------------------------------------------

test(
  'Borrower can confirm demo late fee',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const borrowerToken = await getIdToken(data.borrowerId);

      // Simulate an active booking with a pending late fee.
      await data.bookingRef.update({
        status: 'ACTIVE'
      });

      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,
        phase: 'RETURN_REQUESTED',
        startAt: data.startTime,
        dueAt: data.endTime,
        lateFeeCents: 200,
        simulatedRentalPaid: true,
        simulatedLateFeePaid: false
      });

      const { response, result } =
        await callAuthenticatedFunction(
          'sharelyConfirmDemoLateFee',
          {
            bookingId: data.bookingId
          },
          borrowerToken
        );

      assert.ok(
        response.ok && !result.error,
        `Demo late fee confirmation failed: ${JSON.stringify(result)}`
      );

      // Verify this is a simulated payment.
      assert.equal(result.result.simulated, true);
      assert.equal(result.result.realPaymentProcessed, false);

      // Verify Firestore transaction flow.
      const flowSnapshot = await data.flowRef.get();
      const flow = flowSnapshot.data();

      assert.equal(flow.phase, 'RETURN_REQUESTED');
      assert.equal(flow.simulatedLateFeePaid, true);
      assert.equal(flow.lateFeeCents, 200);

      // Android booking must remain ACTIVE.
      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(bookingSnapshot.data().status, 'ACTIVE');

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test13: Owner issues return OTP
// --------------------------------------------------

test(
  'Owner can issue return OTP',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      // Step 1: Create owner
      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      const ownerToken = await getIdToken(data.ownerId);

      // Step 2: Simulate an active booking
      await data.bookingRef.update({
        status: 'ACTIVE'
      });

      // Step 3: Prepare return-requested transaction
      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,

        phase: 'RETURN_REQUESTED',

        startAt: data.startTime,
        dueAt: data.endTime,

        simulatedRentalPaid: true,
        simulatedLateFeePaid: true,
        lateFeeCents: 0,

        // Simulate an uploaded after-condition photo
        afterPhotos: {
          [data.borrowerId]:
            `transactionEvidence/${data.bookingId}/${data.borrowerId}/after.jpg`
        }
      });

      // Step 4: Owner issues return OTP
      const { response, result } =
        await callAuthenticatedFunction(
          'sharelyIssueReturnOtp',
          {
            bookingId: data.bookingId
          },
          ownerToken
        );

      assert.ok(
        response.ok && !result.error,
        `Issue return OTP failed: ${JSON.stringify(result)}`
      );

      // Step 5: Verify OTP response
      assert.equal(
        typeof result.result.code,
        'string'
      );

      assert.equal(
        result.result.validForSeconds,
        300
      );

      // Step 6: Verify Firestore transaction flow
      const flowSnapshot = await data.flowRef.get();

      assert.equal(flowSnapshot.exists, true);

      const flow = flowSnapshot.data();

      assert.equal(
        flow.phase,
        'RETURN_READY'
      );

      assert.ok(
        flow.returnOtp
      );

      assert.equal(
        typeof flow.returnOtp.hash,
        'string'
      );

      assert.equal(
        flow.returnOtp.failedAttempts,
        0
      );

      // Step 7: Android Booking remains ACTIVE
      const bookingSnapshot =
        await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'ACTIVE'
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test14: Borrower verifies return OTP
// --------------------------------------------------

test(
  'Borrower can verify return OTP and complete booking',
  async () => {
    let data;

    try {
      data = await createTestBooking();

      // Create owner and borrower accounts
      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      const ownerToken = await getIdToken(data.ownerId);
      const borrowerToken = await getIdToken(data.borrowerId);

      // Simulate an active booking
      await data.bookingRef.update({
        status: 'ACTIVE'
      });

      // Prepare return-requested flow
      await data.flowRef.set({
        bookingId: data.bookingId,
        itemId: data.itemId,
        ownerId: data.ownerId,
        borrowerId: data.borrowerId,
        phase: 'RETURN_REQUESTED',
        startAt: data.startTime,
        dueAt: data.endTime,
        simulatedRentalPaid: true,
        simulatedLateFeePaid: true,
        lateFeeCents: 0,
        afterPhotos: {
          [data.borrowerId]:
            `transactionEvidence/${data.bookingId}/${data.borrowerId}/after.jpg`
        }
      });

      // Owner generates return OTP
      const issue = await callAuthenticatedFunction(
        'sharelyIssueReturnOtp',
        { bookingId: data.bookingId },
        ownerToken
      );

      assert.ok(
        issue.response.ok && !issue.result.error,
        `Issue OTP failed: ${JSON.stringify(issue.result)}`
      );

      const code = issue.result.result.code;

      // Borrower verifies return OTP
      const verify = await callAuthenticatedFunction(
        'sharelyVerifyReturnOtp',
        {
          bookingId: data.bookingId,
          code
        },
        borrowerToken
      );

      assert.ok(
        verify.response.ok && !verify.result.error,
        `Verify OTP failed: ${JSON.stringify(verify.result)}`
      );

      assert.equal(verify.result.result.ok, true);
      assert.equal(verify.result.result.phase, 'COMPLETED');

      // Verify transaction flow
      const flowSnapshot = await data.flowRef.get();
      const flow = flowSnapshot.data();

      assert.equal(flow.phase, 'COMPLETED');
      assert.equal(flow.returnOtp, undefined);

      // Verify Android booking
      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'COMPLETED'
      );

      // Verify reservation is removed
      const itemSnapshot = await data.itemRef.get();

      assert.deepEqual(
        itemSnapshot.data().acceptedReservations,
        []
      );

    } finally {
      await cleanupTestData(data);
    }
  }
);

// --------------------------------------------------
// Test15: Public Questions - Ask and Answer
// --------------------------------------------------

test(
  'Borrower can ask public question and owner can answer',
  async () => {
    let data;
    let questionRef;

    try {
      // Step 1: Create test item and booking
      data = await createTestBooking();

      // Step 2: Create owner and borrower accounts
      await auth.createUser({
        uid: data.ownerId,
        email: `${data.ownerId}@example.test`
      });

      await auth.createUser({
        uid: data.borrowerId,
        email: `${data.borrowerId}@example.test`
      });

      // Step 3: Get authentication tokens
      const ownerToken = await getIdToken(data.ownerId);
      const borrowerToken = await getIdToken(data.borrowerId);

      // Step 4: Borrower asks a public question
      const ask = await callAuthenticatedFunction(
        'sharelyAskPublicQuestion',
        {
          itemId: data.itemId,
          question: 'Is this camera available with a charger?'
        },
        borrowerToken
      );

      assert.ok(
        ask.response.ok && !ask.result.error,
        `Ask question failed: ${JSON.stringify(ask.result)}`
      );

      const questionId = ask.result.result.questionId;

      assert.ok(questionId);

      // Step 5: Verify question was saved
      questionRef = db
        .collection('sharelyPublicQuestions')
        .doc(questionId);

      const questionSnapshot = await questionRef.get();

      assert.equal(questionSnapshot.exists, true);

      const question = questionSnapshot.data();

      assert.equal(question.itemId, data.itemId);
      assert.equal(question.ownerId, data.ownerId);
      assert.equal(question.authorId, data.borrowerId);

      assert.equal(
        question.question,
        'Is this camera available with a charger?'
      );

      assert.equal(question.answer, null);

      // Step 6: Borrower attempts to answer
      // This should fail because only the owner can answer.
      const unauthorized = await callAuthenticatedFunction(
        'sharelyAnswerPublicQuestion',
        {
          questionId,
          answer: 'I am pretending to be the owner.'
        },
        borrowerToken
      );

      assert.equal(
        unauthorized.result.error?.status,
        'PERMISSION_DENIED'
      );

      // Step 7: Owner answers the question
      const answer = await callAuthenticatedFunction(
        'sharelyAnswerPublicQuestion',
        {
          questionId,
          answer: 'Yes, the charger is included.'
        },
        ownerToken
      );

      assert.ok(
        answer.response.ok && !answer.result.error,
        `Answer question failed: ${JSON.stringify(answer.result)}`
      );

      assert.equal(answer.result.result.answered, true);

      // Step 8: Verify answer was saved
      const updatedSnapshot = await questionRef.get();

      assert.equal(
        updatedSnapshot.data().answer,
        'Yes, the charger is included.'
      );

      assert.ok(
        updatedSnapshot.data().answeredAt
      );

      // Step 9: Verify Android booking was not modified
      const bookingSnapshot = await data.bookingRef.get();

      assert.equal(
        bookingSnapshot.data().status,
        'ACCEPTED'
      );

    } finally {
      // Clean up question created by the test
      if (questionRef) {
        await questionRef.delete();
      }

      await cleanupTestData(data);
    }
  }
);

    } finally {
      await cleanupTestData(data);
    }
  }
);