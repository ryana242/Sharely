
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require("@firebase/rules-unit-testing");

const {
doc,
setDoc,
updateDoc,
runTransaction
} = require("firebase/firestore");

const fs = require("fs");

const PROJECT_ID = "sharely-7205c";
const OWNER = "owner";
const BORROWER = "borrower";
const ITEM_ID = "test-item";
const BOOKING_ID = "test-booking";

const now = Date.now();
const futureStart = now + 60 * 60 * 1000;
const futureEnd = now + 3 * 60 * 60 * 1000;

let failures = 0;

function makeBooking(
status = "PENDING",
start = futureStart,
end = futureEnd
) {
return {
bookingId: BOOKING_ID,
borrowerId: BORROWER,
itemId: ITEM_ID,
startTime: start,
endTime: end,
status
};
}

function reservation(start = futureStart, end = futureEnd) {
return {
bookingId: BOOKING_ID,
startTime: start,
endTime: end
};
}

async function seed(env, status, ledger = [], start, end) {
await env.clearFirestore();

await env.withSecurityRulesDisabled(async context => {
const db = context.firestore();

await setDoc(doc(db, "items", ITEM_ID), {
itemId: ITEM_ID,
ownerId: OWNER,
itemName: "Test Camera",
description: "Test listing",
category: "Electronics",
price: 20,
location: "Sydney",
status: "ACTIVE",
availabilityStart: start - 1000,
availabilityEnd: end + 1000,
acceptedReservations: ledger
});

await setDoc(
doc(db, "bookings", BOOKING_ID),
makeBooking(status, start, end)
);
});
}

async function changeBookingAndLedger(
db,
status,
ledger
) {
const bookingRef = doc(db, "bookings", BOOKING_ID);
const itemRef = doc(db, "items", ITEM_ID);

return runTransaction(db, async transaction => {
await transaction.get(itemRef);
await transaction.get(bookingRef);

transaction.update(bookingRef, { status });

transaction.update(itemRef, {
acceptedReservations: ledger
});
});
}

async function test(name, operation) {
try {
await operation();
console.log("PASS:", name);
} catch (error) {
failures++;
console.error("FAIL:", name);
console.error("Reason:", error.message);
}
}

async function main() {
const env = await initializeTestEnvironment({
projectId: PROJECT_ID,
firestore: {
host: "127.0.0.1",
port: 8080,
rules: fs.readFileSync("firestore.rules", "utf8")
}
});

try {
const ownerDb =
env.authenticatedContext(OWNER).firestore();

const borrowerDb =
env.authenticatedContext(BORROWER).firestore();

const strangerDb =
env.authenticatedContext("stranger").firestore();

await test(
"Direct acceptance without reservation is blocked",
async () => {
await seed(env, "PENDING", [], futureStart, futureEnd);

await assertFails(
updateDoc(
doc(ownerDb, "bookings", BOOKING_ID),
{ status: "ACCEPTED" }
)
);
}
);

await test(
"Owner cannot insert an arbitrary reservation",
async () => {
await seed(env, "PENDING", [], futureStart, futureEnd);

await assertFails(
updateDoc(
doc(ownerDb, "items", ITEM_ID),
{
acceptedReservations: [
reservation()
]
}
)
);
}
);

await test(
"Owner can accept a booking transactionally",
async () => {
await seed(env, "PENDING", [], futureStart, futureEnd);

await assertSucceeds(
changeBookingAndLedger(
ownerDb,
"ACCEPTED",
[reservation()]
)
);
}
);

await test(
"Owner can reject a pending booking",
async () => {
await seed(env, "PENDING", [], futureStart, futureEnd);

await assertSucceeds(
updateDoc(
doc(ownerDb, "bookings", BOOKING_ID),
{ status: "REJECTED" }
)
);
}
);

await test(
"Borrower can cancel a pending booking",
async () => {
await seed(env, "PENDING", [], futureStart, futureEnd);

await assertSucceeds(
updateDoc(
doc(borrowerDb, "bookings", BOOKING_ID),
{ status: "CANCELLED" }
)
);
}
);

await test(
"Borrower can cancel an accepted booking before pickup",
async () => {
await seed(
env,
"ACCEPTED",
[reservation()],
futureStart,
futureEnd
);

await assertSucceeds(
changeBookingAndLedger(
borrowerDb,
"CANCELLED",
[]
)
);
}
);

await test(
"Owner cannot start a booking before pickup",
async () => {
await seed(
env,
"ACCEPTED",
[reservation()],
futureStart,
futureEnd
);

await assertFails(
updateDoc(
doc(ownerDb, "bookings", BOOKING_ID),
{ status: "ACTIVE" }
)
);
}
);

await test(
"Owner can complete an active booking and release reservation",
async () => {
const start = now - 2 * 60 * 60 * 1000;
const end = now + 60 * 60 * 1000;

await seed(
env,
"ACTIVE",
[reservation(start, end)],
start,
end
);

await assertSucceeds(
changeBookingAndLedger(
ownerDb,
"COMPLETED",
[]
)
);
}
);

await test(
"Unrelated user cannot change booking status",
async () => {
await seed(
env,
"ACCEPTED",
[reservation()],
futureStart,
futureEnd
);

await assertFails(
updateDoc(
doc(strangerDb, "bookings", BOOKING_ID),
{ status: "COMPLETED" }
)
);
}
);

await test(
"Borrower cannot arbitrarily edit item reservations",
async () => {
await seed(
env,
"ACCEPTED",
[reservation()],
futureStart,
futureEnd
);

await assertFails(
updateDoc(
doc(borrowerDb, "items", ITEM_ID),
{ acceptedReservations: [] }
)
);
}
);

console.log("\n-------------------------");
console.log("Booking Security Results");
console.log("-------------------------");
console.log("Passed:", 10 - failures);
console.log("Failed:", failures);

if (failures > 0) {
process.exitCode = 1;
}
} finally {
await env.cleanup();
}
}

main().catch(error => {
console.error("Test suite error:", error);
process.exitCode = 1;
});
