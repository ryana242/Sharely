'use strict';
const crypto = require('node:crypto');
const OTP_MILLIS = 5 * 60 * 1000;
function cents(n) {
  return Number.isSafeInteger(n) && n >= 0 && n <= 10_000_000_000;
}
function fee(due, returned, hourly, graceMinutes, maxFee) {
  for (const n of [due, returned, hourly, graceMinutes, maxFee]) {
    if (!Number.isSafeInteger(n) || n < 0) throw new TypeError('Invalid fee inputs');
  }
  if (!cents(hourly) || !cents(maxFee) || graceMinutes > 43200) throw new TypeError('Fee policy exceeds bounds');
  const late = Math.max(0, returned - (due + graceMinutes * 60_000));
  return late ? Math.min(maxFee, Math.ceil(late / 3_600_000) * hourly) : 0;
}
function freshOtp(bookingId, now) {
  const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, '0');
  const salt = crypto.randomBytes(20).toString('hex');
  return {code, salt, hash: digest(bookingId, code, salt), expiresAt: now + OTP_MILLIS};
}
function digest(bookingId, code, salt) {
  return crypto.createHash('sha256').update(bookingId + ':' + code + ':' + salt).digest('hex');
}
function matches(bookingId, code, stored) {
  if (!stored || !/^\d{6}$/.test(String(code))) return false;
  const expected = Buffer.from(stored.hash, 'hex');
  const actual = Buffer.from(digest(bookingId, code, stored.salt), 'hex');
  return expected.length === actual.length && crypto.timingSafeEqual(expected, actual);
}
function milliseconds(value) {
  if (Number.isSafeInteger(value)) return value;
  if (value && typeof value.toMillis === 'function') return value.toMillis();
  if (value && Number.isInteger(value.seconds)) return value.seconds * 1000;
  if (typeof value === 'string') {
    const t = Date.parse(value);
    if (!Number.isNaN(t)) return t;
  }
  return NaN;
}
function bookingDetails(raw, ownerId) {
  const borrowerId = raw.borrowerId;
  const itemId = raw.itemId;
  const startAt = milliseconds(raw.startTime);
  const endAt = milliseconds(raw.endTime);
  if (!ownerId || !borrowerId || !itemId || ownerId === borrowerId
      || !Number.isSafeInteger(startAt) || !Number.isSafeInteger(endAt)
      || endAt <= startAt)
    throw new TypeError('Booking shape mismatch');
  return {ownerId, borrowerId, itemId, startAt, dueAt:endAt,
      status:String(raw.status || '').toUpperCase()};
}
function rentalQuote(price, startAt, endAt) {
  if (!Number.isFinite(price) || price < 0 || price > 100000
      || !Number.isSafeInteger(startAt) || !Number.isSafeInteger(endAt)
      || endAt <= startAt) throw new TypeError('Invalid rental quote');
  return Math.ceil((endAt-startAt) / 86400000) * Math.round(price*100);
}
function latePolicy(price) {
  if (!Number.isFinite(price) || price < 0 || price > 100000)
    throw new TypeError('Invalid late fee price');
  const daily=Math.round(price*100);
  return {hourly:Math.ceil(daily*.10),grace:15,cap:daily};
}
module.exports = {OTP_MILLIS, cents, fee, freshOtp, matches, milliseconds, bookingDetails, rentalQuote, latePolicy};
