'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const h = require('./helpers.js');
test('OTP length, expiry, hash verification', () => {
  const x = h.freshOtp('booking', 1234);
  assert.match(x.code, /^\d{6}$/);
  assert.equal(x.expiresAt, 1234 + 300000);
  assert.equal(h.matches('booking', x.code, x), true);
  assert.equal(h.matches('other', x.code, x), false);
  assert.equal(h.matches('booking', 'abcdef', x), false);
});
test('late-fee rules', () => {
  assert.equal(h.fee(100000,100000,100,0,400),0);
  assert.equal(h.fee(100000,100001,100,0,400),100);
  assert.equal(h.fee(100000,100000+3600001,100,0,400),200);
  assert.equal(h.fee(100000,100000+20*3600000,100,0,400),400);
  assert.equal(h.fee(100000,100000+29*60000,100,30,400),0);
  assert.throws(()=>h.fee(1,2,-1,0,100));
});
test('booking adapter basic shape and fail closed', () => {
  const b=h.bookingDetails({itemId:'item',borrowerId:'b',startTime:100000,endTime:200000,status:'accepted'}, 'a');
  assert.equal(b.status,'ACCEPTED');
  assert.equal(b.dueAt,200000);
  assert.throws(()=>h.bookingDetails({itemId:'item',borrowerId:'b'},'a'));
  assert.equal(b.itemId,'item');
  assert.equal(b.startAt,100000);
});

test('rental quotes charge whole or partial days as demo cents', () => {
  assert.equal(h.rentalQuote(4,1000,1000+60*60*1000),400);
  assert.equal(h.rentalQuote(4,1000,1000+25*60*60*1000),800);
  assert.throws(()=>h.rentalQuote(-1,0,1000));
  assert.throws(()=>h.rentalQuote(4,10,10));
});
test('simulated late-fee policy has grace and a daily cap', () => {
  const p=h.latePolicy(10);
  assert.deepEqual(p,{hourly:100,grace:15,cap:1000});
  const due=1_000_000;
  assert.equal(h.fee(due,due+15*60_000,p.hourly,p.grace,p.cap),0);
  assert.equal(h.fee(due,due+16*60_000,p.hourly,p.grace,p.cap),100);
  assert.equal(h.fee(due,due+20*60*60_000,p.hourly,p.grace,p.cap),1000);
});
