const fs = require("fs");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require("@firebase/rules-unit-testing");

const {
  ref,
  uploadBytes,
  deleteObject,
  getBytes
} = require("firebase/storage");

const {
  doc,
  setDoc
} = require("firebase/firestore");

let passed = 0;
let failed = 0;

// ============================================
// Run one test
// ============================================

async function runTest(name, testFunction) {
  try {
    await testFunction();

    console.log(`PASS: ${name}`);
    passed++;

  } catch (error) {
    console.log(`FAIL: ${name}`);
    console.log(`Reason: ${error.message}`);
    failed++;
  }
}

// ============================================
// Print test summary
// ============================================

function printSummary() {
  console.log("\n======================================");
  console.log("Firebase Storage Security Test Results");
  console.log("======================================");

  console.log(`Total Tests: ${passed + failed}`);
  console.log(`Passed: ${passed}`);
  console.log(`Failed: ${failed}`);

  console.log("======================================");

  if (failed > 0) {
    process.exitCode = 1;
  }
}

// ============================================
// Main
// ============================================

async function main() {

  // Initialize Firebase Emulator environment
  const env = await initializeTestEnvironment({

    projectId: "sharely-7205c",
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8")
    },

    storage: {
      rules: fs.readFileSync("storage.rules", "utf8")
    }

  });

  try {

    // ============================================
    // STEP 1: Create Firestore test item
    // ============================================

    await env.withSecurityRulesDisabled(async (context) => {

      const db = context.firestore();

      await setDoc(doc(db, "items", "item123"), {

        itemId: "item123",
        ownerId: "alice",
        itemName: "Camera"

      });

    });

    console.log("Firestore test item created successfully.");
const { getDoc } = require("firebase/firestore");

const testDb = env.authenticatedContext("alice").firestore();

const itemSnapshot = await getDoc(
  doc(testDb, "items", "item123")
);

console.log("Item exists:", itemSnapshot.exists());
console.log("Item data:", itemSnapshot.data());
    // ============================================
    // STEP 2: Create authenticated users
    // ============================================

    const ownerStorage =
      env.authenticatedContext("alice").storage();

    const strangerStorage =
      env.authenticatedContext("bob").storage();

    const guestStorage =
      env.unauthenticatedContext().storage();

    // ============================================
    // STEP 3: Prepare image data
    // ============================================

    const imageData = new Uint8Array([1, 2, 3, 4]);

    // IMPORTANT:
    // Match storage.rules:
    // items/{ownerId}/{itemId}/{imageName}

    const imagePath = "items/item123/image_0.jpg";

    const otherImagePath = "items/item123/image_1.jpg";

    const imageRef = ref(ownerStorage, imagePath);

    // ============================================
    // TEST 1: Owner can upload image
    // ============================================

    await runTest("Owner can upload image", async () => {

      await assertSucceeds(
        uploadBytes(
          imageRef,
          imageData,
          {
            contentType: "image/jpeg"
          }
        )
      );

    });

    // ============================================
    // TEST 2: Stranger cannot upload image
    // ============================================

    await runTest("Stranger cannot upload image", async () => {

      await assertFails(
        uploadBytes(
          ref(strangerStorage, otherImagePath),
          imageData,
          {
            contentType: "image/jpeg"
          }
        )
      );

    });

    // ============================================
    // TEST 3: Owner can replace image
    // ============================================

    await runTest("Owner can replace image", async () => {

      await assertSucceeds(
        uploadBytes(
          imageRef,
          imageData,
          {
            contentType: "image/jpeg"
          }
        )
      );

    });

    // ============================================
    // TEST 4: Stranger cannot replace image
    // ============================================

    await runTest("Stranger cannot replace image", async () => {

      await assertFails(
        uploadBytes(
          ref(strangerStorage, imagePath),
          imageData,
          {
            contentType: "image/jpeg"
          }
        )
      );

    });

    // ============================================
    // TEST 5: Stranger cannot delete image
    // ============================================

    await runTest("Stranger cannot delete image", async () => {

      await assertFails(
        deleteObject(
          ref(strangerStorage, imagePath)
        )
      );

    });

    // ============================================
    // TEST 6: Guest cannot upload image
    // ============================================

    await runTest("Unauthenticated user cannot upload", async () => {

      await assertFails(
        uploadBytes(
          ref(guestStorage, otherImagePath),
          imageData,
          {
            contentType: "image/jpeg"
          }
        )
      );

    });

    // ============================================
    // TEST 7: Oversized image is rejected
    // ============================================

    await runTest("Oversized image is rejected", async () => {

      const largeImage = new Uint8Array(
        5 * 1024 * 1024
      );

      await assertFails(
        uploadBytes(
          ref(ownerStorage, otherImagePath),
          largeImage,
          {
            contentType: "image/jpeg"
          }
        )
      );

    });

    // ============================================
    // TEST 8: Non-image upload is rejected
    // ============================================

    await runTest("Non-image upload is rejected", async () => {

      await assertFails(
        uploadBytes(
          ref(ownerStorage, otherImagePath),
          imageData,
          {
            contentType: "text/plain"
          }
        )
      );

    });

    // ============================================
    // TEST 9: Owner can read image
    // ============================================

    await runTest("Owner can read image", async () => {

      await assertSucceeds(
        getBytes(imageRef)
      );

    });

    // ============================================
    // TEST 10: Stranger can read image
    // ============================================

    await runTest("Stranger can read image", async () => {

      await assertSucceeds(
        getBytes(
          ref(strangerStorage, imagePath)
        )
      );

    });

    // ============================================
    // TEST 11: Guest cannot read image
    // ============================================

    await runTest("Unauthenticated user cannot read image", async () => {

      await assertFails(
        getBytes(
          ref(guestStorage, imagePath)
        )
      );

    });

    // ============================================
    // TEST 12: Owner can delete image
    // ============================================

    await runTest("Owner can delete image", async () => {

      await assertSucceeds(
        deleteObject(imageRef)
      );

    });

    // ============================================
    // TEST 13: Prevent fake ownership
    // ============================================

    // Bob tries to upload an image for Alice's item
    // by placing his own UID in the Storage path.

    await runTest(
      "Stranger cannot upload another image",
      async () => {
        await assertFails(
          uploadBytes(
            ref(strangerStorage, "items/item123/image_2.jpg"),
            imageData,
            { contentType: "image/jpeg" }
          )
        );
      }
    );

// ============================================
// STEP 4: Transaction Evidence Security Tests
// ============================================

// Create a test booking
await env.withSecurityRulesDisabled(async (context) => {
  const db = context.firestore();

  await setDoc(doc(db, "bookings", "booking123"), {
    bookingId: "booking123",
    borrowerId: "charlie",
    itemId: "item123",
    startTime: Date.now() + 86400000,
    endTime: Date.now() + 172800000,
    status: "ACCEPTED"
  });
});

// Alice = Owner
// Charlie = Borrower
// Bob = Stranger

const borrowerStorage =
  env.authenticatedContext("charlie").storage();

const ownerEvidencePath =
  "transactionEvidence/booking123/alice/before.jpg";

const borrowerEvidencePath =
  "transactionEvidence/booking123/charlie/after.jpg";

const strangerEvidencePath =
  "transactionEvidence/booking123/bob/before.jpg";

const ownerEvidenceRef =
  ref(ownerStorage, ownerEvidencePath);

const borrowerEvidenceRef =
  ref(borrowerStorage, borrowerEvidencePath);

// ============================================
// TEST 14: Owner uploads evidence
// ============================================

await runTest("Owner can upload transaction evidence", async () => {
  await assertSucceeds(
    uploadBytes(ownerEvidenceRef, imageData, {
      contentType: "image/jpeg"
    })
  );
});

// ============================================
// TEST 15: Borrower uploads evidence
// ============================================

await runTest("Borrower can upload transaction evidence", async () => {
  await assertSucceeds(
    uploadBytes(borrowerEvidenceRef, imageData, {
      contentType: "image/jpeg"
    })
  );
});

// ============================================
// TEST 16: Stranger cannot upload evidence
// ============================================

await runTest("Stranger cannot upload transaction evidence", async () => {
  await assertFails(
    uploadBytes(
      ref(strangerStorage, strangerEvidencePath),
      imageData,
      { contentType: "image/jpeg" }
    )
  );
});

// ============================================
// TEST 17: Owner can read evidence
// ============================================

await runTest("Owner can read transaction evidence", async () => {
  await assertSucceeds(
    getBytes(
      ref(ownerStorage, borrowerEvidencePath)
    )
  );
});

// ============================================
// TEST 18: Borrower can read owner's evidence
// ============================================

await runTest("Borrower can read owner's evidence", async () => {
  await assertSucceeds(
    getBytes(
      ref(borrowerStorage, ownerEvidencePath)
    )
  );
});

// ============================================
// TEST 19: Stranger cannot read evidence
// ============================================

await runTest("Stranger cannot read transaction evidence", async () => {
  await assertFails(
    getBytes(
      ref(strangerStorage, ownerEvidencePath)
    )
  );
});

// ============================================
// TEST 20: Guest cannot read evidence
// ============================================

await runTest("Guest cannot read transaction evidence", async () => {
  await assertFails(
    getBytes(
      ref(guestStorage, ownerEvidencePath)
    )
  );
});

// ============================================
// TEST 21: Borrower cannot delete evidence
// ============================================

await runTest("Borrower cannot delete transaction evidence", async () => {
  await assertFails(
    deleteObject(borrowerEvidenceRef)
  );
});

// ============================================
// TEST 22: Borrower cannot upload under owner's UID
// ============================================

await runTest("Borrower cannot impersonate owner in evidence path", async () => {
  await assertFails(
    uploadBytes(
      ref(
        borrowerStorage,
        "transactionEvidence/booking123/alice/fake.jpg"
      ),
      imageData,
      { contentType: "image/jpeg" }
    )
  );
});

  } finally {

    // Clean up the test environment
    await env.cleanup();

    // Print results
    printSummary();

  }
}

// ============================================
// Start
// ============================================

main().catch(error => {

  console.error("Test setup failed:", error);

  process.exitCode = 1;

});