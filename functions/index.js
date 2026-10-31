'use strict';
/**
 * Sharely backend extension, NOT DEPLOYED.
 * Must be mapped to the real Booking fields and reviewed alongside existing rules.
 * All payments here are clearly marked DEMO ONLY: no funds are captured.
 */
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const sharelyCall = handler => onCall({region:'australia-southeast1'},handler);
const admin = require('firebase-admin');
const {FieldValue} = require('firebase-admin/firestore');
const {fee, cents, freshOtp, matches, bookingDetails, rentalQuote, latePolicy} = require('./helpers.js');
if (!admin.apps.length) admin.initializeApp();
const db = admin.firestore();
const FLOW_COLLECTION = 'sharelyTransactionFlows';
const BOOKING_COLLECTION = 'bookings';
const ITEM_COLLECTION = 'items';
const now = () => Date.now();
const reqAuth = (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError('unauthenticated', 'Please sign in.');
  return uid;
};
const idOf = (request, name = 'bookingId') => {
  const id = request.data && request.data[name];
  if (typeof id !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(id))
    throw new HttpsError('invalid-argument', 'Invalid ' + name);
  return id;
};
const fail = (code, msg) => { throw new HttpsError(code, msg); };
const participant = (f, uid) => {
  if (f.ownerId !== uid && f.borrowerId !== uid) fail('permission-denied','Not a transaction participant');
};
const owner = (f, uid) => {
  if (f.ownerId !== uid) fail('permission-denied','Owner-only action');
};
const borrower = (f, uid) => {
  if (f.borrowerId !== uid) fail('permission-denied','Borrower-only action');
};
const stage = (f, ...allowed) => {
  if (!allowed.includes(f.phase)) fail('failed-precondition','Invalid transaction stage: '+f.phase);
};
const refFor = id => db.collection(FLOW_COLLECTION).doc(id);
const bookFor = id => db.collection(BOOKING_COLLECTION).doc(id);
function publicView(f) {
  const {pickupOtp,returnOtp,...safe} = f;
  // Metadata/condition photos are private: callers must already be participants.
  return safe;
}
async function mutateFlow(id, uid, fn) {
  const ref=refFor(id);
  return db.runTransaction(async tx => {
    const snap=await tx.get(ref);
    if (!snap.exists) fail('not-found','Transaction not initialised');
    const f=snap.data();
    participant(f,uid);
    return fn(tx,ref,f);
  });
}
function event(tx,bookingId,eventType,uid) {
  const ev=db.collection('sharelyTransactionEvents').doc();
  tx.create(ev,{bookingId,eventType,actorId:uid,createdAt:FieldValue.serverTimestamp()});
}
function validateFutureMs(n,label) {
  if (!Number.isSafeInteger(n) || n <= now())
    fail('invalid-argument',label+' must be a future epoch-millisecond timestamp');
}
async function verifiedPhotoPath(bookingId, uid, path) {
  if (typeof path !== 'string' || !path.startsWith(`transactionEvidence/${bookingId}/${uid}/`)
      || path.includes('..') || path.length > 500)
    fail('invalid-argument','Invalid private evidence path');
  const obj=admin.storage().bucket().file(path);
  const [exists]=await obj.exists();
  if (!exists) fail('failed-precondition','Photo must be uploaded to Storage first');
  const [meta]=await obj.getMetadata();
  if (!String(meta.contentType||'').startsWith('image/') || Number(meta.size)>10_000_000)
    fail('failed-precondition','Expected an image under 10 MB');
  return path;
}
function bookingPolicy(raw) {
  // Simulation policy: 10% daily rent per started late hour;
  // 15-minute grace period; cap one day's rent.
  try { return latePolicy(Number(raw.price)); }
  catch (_) { fail('failed-precondition','Invalid listing price'); }
}

exports.sharelyStartFlow = sharelyCall(async request => {
  const uid=reqAuth(request), id=idOf(request);
  return db.runTransaction(async tx => {
    const bookingSnap=await tx.get(bookFor(id));
    if (!bookingSnap.exists) fail('not-found','Booking does not exist');
    const raw=bookingSnap.data();
    if (typeof raw.itemId !== 'string') fail('failed-precondition','Booking has no item');
    const itemSnap=await tx.get(db.collection('items').doc(raw.itemId));
    const flowSnap=await tx.get(refFor(id));
    if (!itemSnap.exists) fail('not-found','Item missing');
    let b;
    try { b=bookingDetails(raw,itemSnap.data().ownerId); }
    catch (_) { fail('failed-precondition','Booking data invalid'); }
    if (uid!==b.ownerId && uid!==b.borrowerId) fail('permission-denied','Not a booking participant');
    if (flowSnap.exists) return publicView(flowSnap.data());
    if (b.status!=='ACCEPTED') fail('failed-precondition','Booking must be ACCEPTED');
    const latePolicy=bookingPolicy(itemSnap.data());
    const rentalCents=rentalQuote(Number(itemSnap.data().price), b.startAt, b.dueAt);
    const f={bookingId:id,itemId:b.itemId,ownerId:b.ownerId,borrowerId:b.borrowerId,
      phase:'ACCEPTED',startAt:b.startAt,dueAt:b.dueAt,createdAt:now(),latePolicy,
      rentalCents,simulatedRentalPaid:false, simulatedLateFeePaid:false,lateFeeCents:0};
    tx.create(refFor(id),f); event(tx,id,'TRANSACTION_PREPARED',uid);
    return publicView(f);
  });
});

exports.sharelyGetFlow = sharelyCall(async request => {
  const uid=reqAuth(request), id=idOf(request);
  const snap=await refFor(id).get();
  if (!snap.exists) fail('not-found','Transaction not found');
  participant(snap.data(),uid);
  return publicView(snap.data());
});

exports.sharelySchedulePickup = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request),time=request.data.pickupAt;
  validateFutureMs(time,'Pickup');
  return mutateFlow(id,uid,(tx,ref,f)=>{
    owner(f,uid); stage(f,'ACCEPTED','PICKUP_SCHEDULED');
    if (time < f.startAt || time >= f.dueAt) fail('invalid-argument','Pickup must be inside booked period');
    tx.update(ref,{phase:'PICKUP_SCHEDULED',pickupAt:time,updatedAt:now()});
    event(tx,id,'PICKUP_SCHEDULED',uid); return {phase:'PICKUP_SCHEDULED'};
  });
});

exports.sharelyAttachBeforePhoto = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  const path=await verifiedPhotoPath(id,uid,request.data.storagePath);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    owner(f,uid); stage(f,'PICKUP_SCHEDULED','PICKUP_READY');
    tx.update(ref,{phase:'PICKUP_READY',beforePhotoPath:path,updatedAt:now()});
    event(tx,id,'BEFORE_CONDITION_RECORDED',uid); return {phase:'PICKUP_READY'};
  });
});

exports.sharelyConfirmDemoRentalPayment = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    borrower(f,uid); stage(f,'PICKUP_READY');
    tx.update(ref,{simulatedRentalPaid:true,updatedAt:now()});
    event(tx,id,'DEMO_RENTAL_PAYMENT_RECORDED',uid);
    return {simulated:true,realPaymentProcessed:false};
  });
});

exports.sharelyIssuePickupOtp = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    borrower(f,uid); stage(f,'PICKUP_READY');
    if (!f.beforePhotoPath || !f.simulatedRentalPaid)
      fail('failed-precondition','Before photo and demo payment required');
    const t=now();
    if (f.pickupOtp && t < f.pickupOtp.issuedAt+30000)
      fail('resource-exhausted','Retry after 30 seconds');
    const otp=freshOtp(id,t);
    tx.update(ref,{pickupOtp:{hash:otp.hash,salt:otp.salt,
      expiresAt:otp.expiresAt,issuedAt:t,failedAttempts:0},updatedAt:t});
    event(tx,id,'PICKUP_OTP_ISSUED',uid);
    // Show code to borrower ONLY, to be given verbally to owner.
    return {code:otp.code,validForSeconds:300};
  });
});

exports.sharelyVerifyPickupOtp = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request),code=request.data.code;
  const result=await db.runTransaction(async tx=>{
    const bookingSnap=await tx.get(bookFor(id));
    const flowSnap=await tx.get(refFor(id));
    if (!bookingSnap.exists || !flowSnap.exists) fail('not-found','Booking/flow missing');
    const f=flowSnap.data(); owner(f,uid); stage(f,'PICKUP_READY');
    if (String(bookingSnap.data().status).toUpperCase()!=='ACCEPTED')
      fail('failed-precondition','Booking no longer ACCEPTED');
    if (!f.beforePhotoPath || !f.simulatedRentalPaid || !f.pickupOtp)
      fail('failed-precondition','Pickup prerequisites missing');
    const otp=f.pickupOtp, t=now();
    if (t>=otp.expiresAt || otp.failedAttempts>=5)
      fail('failed-precondition','OTP expired or locked; issue a new one');
    if (!matches(id,code,otp)) {
      tx.update(refFor(id),{'pickupOtp.failedAttempts':otp.failedAttempts+1});
      return {ok:false};
    }
    tx.update(refFor(id),{phase:'ACTIVE',pickupOtp:FieldValue.delete(),
      actualPickupAt:t,updatedAt:t});
    tx.update(bookFor(id),{status:'ACTIVE'});
    event(tx,id,'BORROWING_STARTED',uid);
    return {ok:true,phase:'ACTIVE'};
  });
  if (!result.ok) fail('permission-denied','Incorrect pickup OTP');
  return result;
});

exports.sharelyScheduleReturn = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request),time=request.data.returnAt;
  validateFutureMs(time,'Return');
  return mutateFlow(id,uid,(tx,ref,f)=>{
    stage(f,'ACTIVE');
    tx.update(ref,{scheduledReturnAt:time,updatedAt:now()});
    event(tx,id,'RETURN_SCHEDULED',uid); return {scheduledReturnAt:time};
  });
});

exports.sharelyRequestReturn = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    borrower(f,uid); stage(f,'ACTIVE');
    if (!f.scheduledReturnAt) fail('failed-precondition','Schedule return first');
    const t=now(),p=f.latePolicy||{hourly:0,grace:0,cap:0};
    const late=fee(f.dueAt,t,p.hourly,p.grace,p.cap);
    tx.update(ref,{phase:'RETURN_REQUESTED',actualReturnRequestedAt:t,
      lateFeeCents:late,simulatedLateFeePaid:late===0,updatedAt:t});
    event(tx,id,late>0?'LATE_FEE_DUE':'RETURN_REQUESTED',uid);
    return {phase:'RETURN_REQUESTED',lateFeeCents:late,
      realPaymentProcessed:false};
  });
});

exports.sharelyConfirmDemoLateFee = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    borrower(f,uid); stage(f,'RETURN_REQUESTED','RETURN_READY');
    tx.update(ref,{simulatedLateFeePaid:true,updatedAt:now()});
    event(tx,id,'DEMO_LATE_FEE_PAYMENT_RECORDED',uid);
    return {simulated:true,realPaymentProcessed:false};
  });
});

exports.sharelyIssueReturnOtp = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    owner(f,uid); stage(f,'RETURN_REQUESTED','RETURN_READY');
    if (!f.simulatedLateFeePaid || !f.afterPhotos || !Object.keys(f.afterPhotos).length)
      fail('failed-precondition','Post-borrow photo and demo late fee required');
    const t=now();
    if (f.returnOtp && t < f.returnOtp.issuedAt+30000)
      fail('resource-exhausted','Retry after 30 seconds');
    const otp=freshOtp(id,t);
    tx.update(ref,{phase:'RETURN_READY',returnOtp:{hash:otp.hash,salt:otp.salt,
      expiresAt:otp.expiresAt,issuedAt:t,failedAttempts:0},updatedAt:t});
    event(tx,id,'RETURN_OTP_ISSUED',uid);
    // Owner receives returned item, then shows this code to borrower.
    return {code:otp.code,validForSeconds:300};
  });
});

exports.sharelyVerifyReturnOtp = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request),code=request.data.code;
  const result=await db.runTransaction(async tx=>{
    const bookingSnap=await tx.get(bookFor(id));
    const flowSnap=await tx.get(refFor(id));
    if (!bookingSnap.exists || !flowSnap.exists) fail('not-found','Booking/flow missing');
    const f=flowSnap.data(); borrower(f,uid); stage(f,'RETURN_READY');
    const itemRef=db.collection('items').doc(f.itemId);
    const itemSnap=await tx.get(itemRef);
    if (!itemSnap.exists || itemSnap.data().ownerId!==f.ownerId)
      fail('failed-precondition','Listing no longer valid');
    if (String(bookingSnap.data().status).toUpperCase()!=='ACTIVE')
      fail('failed-precondition','Booking must be ACTIVE');
    if (!f.simulatedLateFeePaid || !f.returnOtp || !f.afterPhotos
        || !Object.keys(f.afterPhotos).length)
      fail('failed-precondition','Return prerequisites missing');
    const otp=f.returnOtp,t=now();
    if (t>=otp.expiresAt || otp.failedAttempts>=5)
      fail('failed-precondition','Return OTP expired or locked');
    if (!matches(id,code,otp)) {
      tx.update(refFor(id),{'returnOtp.failedAttempts':otp.failedAttempts+1});
      return {ok:false};
    }
    const ledger=itemSnap.data().acceptedReservations || [];
    const matchesReservation = entry => entry.bookingId === id &&
      entry.startTime === bookingSnap.data().startTime &&
      entry.endTime === bookingSnap.data().endTime;
    if (ledger.filter(matchesReservation).length !== 1)
      fail('failed-precondition','Reservation ledger is inconsistent');
    tx.update(itemRef,{acceptedReservations:ledger.filter(entry=>!matchesReservation(entry))});
    tx.update(refFor(id),{phase:'COMPLETED',returnOtp:FieldValue.delete(),
      actualReturnConfirmedAt:t,updatedAt:t});
    tx.update(bookFor(id),{status:'COMPLETED'});
    event(tx,id,'BOOKING_COMPLETED',uid);
    return {ok:true,phase:'COMPLETED'};
  });
  if (!result.ok) fail('permission-denied','Incorrect return OTP');
  return result;
});

exports.sharelyAttachAfterPhoto = sharelyCall(async request => {
  const uid=reqAuth(request),id=idOf(request);
  const path=await verifiedPhotoPath(id,uid,request.data.storagePath);
  return mutateFlow(id,uid,(tx,ref,f)=>{
    borrower(f,uid); stage(f,'RETURN_REQUESTED','RETURN_READY');
    const newPhotos={...(f.afterPhotos||{}),[uid]:path};
    tx.update(ref,{afterPhotos:newPhotos,updatedAt:now()});
    event(tx,id,'AFTER_CONDITION_RECORDED',uid);
    return {recorded:true};
  });
});

exports.sharelyAskPublicQuestion = sharelyCall(async request => {
  const uid = reqAuth(request);
  const itemId = idOf(request, 'itemId');
  const question = request.data.question;

  if (
    typeof question !== 'string' ||
    question.trim().length < 3 ||
    question.trim().length > 500
  ) {
    fail('invalid-argument', 'Question must be 3-500 characters');
  }

  const item = await db.collection(ITEM_COLLECTION).doc(itemId).get();

  if (!item.exists) {
    fail('not-found', 'Item not found');
  }

  const ownerId = item.data().ownerId || item.data().userId;

  if (!ownerId) {
    fail('failed-precondition', 'Listing owner not found');
  }

  if (uid === ownerId) {
    fail('permission-denied', 'Owners cannot ask their own listings');
  }

  const ref = db.collection('itemQuestions').doc();

  await ref.create({
    questionId: ref.id,
    itemId,
    authorId: uid,
    question: question.trim(),
    answer: '',
    createdAt: Date.now()
  });

  return { questionId: ref.id };
});

exports.sharelyAnswerPublicQuestion = sharelyCall(async request => {
  const uid = reqAuth(request);
  const id = idOf(request, 'questionId');
  const answer = request.data.answer;

  if (
    typeof answer !== 'string' ||
    answer.trim().length < 2 ||
    answer.trim().length > 1000
  ) {
    fail('invalid-argument', 'Answer must be 2-1000 characters');
  }

  const ref = db.collection('itemQuestions').doc(id);

  await db.runTransaction(async tx => {
    const snap = await tx.get(ref);

    if (!snap.exists) {
      fail('not-found', 'Question not found');
    }

    const data = snap.data();

    const item = await tx.get(
      db.collection(ITEM_COLLECTION).doc(data.itemId)
    );

    if (!item.exists) {
      fail('not-found', 'Item not found');
    }

    const ownerId = item.data().ownerId || item.data().userId;

    if (ownerId !== uid) {
      fail('permission-denied', 'Only listing owner may answer');
    }

    if (data.answer !== '') {
      fail('failed-precondition', 'Question already answered');
    }

    tx.update(ref, {
      answer: answer.trim(),
      answeredAt: Date.now()
    });
  });

  return { answered: true };
});

exports.sharelyReportContent = sharelyCall(async request => {
  const uid=reqAuth(request),targetId=idOf(request,'targetId');
  const {targetType,reason,description}=request.data;
  if (!['ITEM','ACCOUNT'].includes(targetType) ||
      !['ILLEGAL_ITEM','MISLEADING_LISTING','HARASSMENT','DAMAGE','OTHER'].includes(reason) ||
      typeof description!=='string' || !description.trim() || description.length>2000)
    fail('invalid-argument','Invalid report');
  const ref=db.collection('sharelyReports').doc();
  await ref.create({reporterId:uid,targetId,targetType,reason,description:description.trim(),
    status:'OPEN',createdAt:FieldValue.serverTimestamp()});
  // Reports remain private for admin moderation, with no automatic police disclosure.
  return {reportId:ref.id,submitted:true};
});

// New transaction events generate recipient-scoped inbox notices. Admin writes
// bypass client create rules; duplicate deliveries are prevented by event ID.
const NOTICE = {
  PICKUP_SCHEDULED:['PICKUP_SCHEDULED','Pickup arranged','Your pickup meeting was scheduled'],
  DEMO_RENTAL_PAYMENT_RECORDED:['DEMO_PAYMENT_CONFIRMED','Demo payment confirmed','A simulated rental payment was recorded'],
  BORROWING_STARTED:['PICKUP_CONFIRMED','Pickup verified','Your borrowing has started'],
  RETURN_REQUESTED:['RETURN_REQUESTED','Return requested','The borrower requested to return your item'],
  LATE_FEE_DUE:['RETURN_REQUESTED','Return requested','A demo late fee was calculated'],
  BOOKING_COMPLETED:['BOOKING_COMPLETED','Return verified','The item return is complete']
};
exports.sharelyNotifyTransaction = onDocumentCreated({document:'sharelyTransactionEvents/{eventId}',region:'australia-southeast1'}, async event => {
  if (!event.data) return;
  const data=event.data.data();
  const description=NOTICE[data.eventType];
  if (!description) return;
  const flow=await refFor(data.bookingId).get();
  if(!flow.exists) return;
  const f=flow.data();
  if(data.actorId!==f.ownerId && data.actorId!==f.borrowerId) return;
  const recipientId=data.actorId===f.ownerId ? f.borrowerId : f.ownerId;
  if (!recipientId) return;
  const ref=db.collection('notifications').doc('transaction_'+event.params.eventId);
  await ref.create({notificationId:ref.id,recipientId,senderId:data.actorId,
    type:description[0],title:description[1],message:description[2],
    relatedId:data.bookingId,isRead:false,createdAt:Date.now()}).catch(e=>{
      if(e.code!==6 && e.code!=='already-exists') throw e;
  });
});
