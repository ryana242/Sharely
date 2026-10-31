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

    projectId: "sharely-storage-test",

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
      "Stranger cannot upload image using their own UID for someone else's item",
      async () => {

        await assertFails(
          uploadBytes(
            ref(
              strangerStorage,
              "items/bob/item123/image_0.jpg"
            ),
            imageData,
            {
              contentType: "image/jpeg"
            }
          )
        );

      }
    );

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