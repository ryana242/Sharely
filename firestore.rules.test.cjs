const {
    initializeTestEnvironment,
    assertSucceeds,
    assertFails
} = require("@firebase/rules-unit-testing");

const {
    doc,
    setDoc,
    updateDoc,
    getDoc,
    deleteDoc
} = require("firebase/firestore");

const fs = require("fs");

async function main() {

    const testEnv = await initializeTestEnvironment({
        projectId: "sharely-7205c",
        firestore: {
            host: "127.0.0.1",
            port: 8080,
            rules: fs.readFileSync("firestore.rules", "utf8")
        }
    });

    try {
        // Clear previous test data
        await testEnv.clearFirestore();

        // Create test users without security restrictions
        await testEnv.withSecurityRulesDisabled(async context => {

            const db = context.firestore();

            await setDoc(doc(db, "users", "userA"), {
                name: "Alice",
                email: "alice@example.com",
                rating: 0,
                transactionCount: 0
            });

            await setDoc(doc(db, "users", "userB"), {
                name: "Bob",
                email: "bob@example.com",
                rating: 0,
                transactionCount: 0
            });

        // Create an item owned by userA
        await setDoc(doc(db, "items", "bookingItem1"), {
            itemId: "bookingItem1",
            ownerId: "userA",
            itemName: "Camera",
            description: "Camera for rent",
            category: "Electronics",
            price: 20,
            location: "Sydney",
            status: "ACTIVE"
        });

        // Create a pending booking by userB
        await setDoc(doc(db, "bookings", "booking1"), {
            bookingId: "booking1",
            borrowerId: "userB",
            itemId: "bookingItem1",
            startTime: 1000,
            endTime: 2000,
            status: "PENDING"
        });

        });

        // Simulate User A
        const userA = testEnv.authenticatedContext("userA");
        const dbA = userA.firestore();

        // Test 1: User A updates own profile
        await assertSucceeds(
            updateDoc(doc(dbA, "users", "userA"), {
                name: "Alice Updated"
            })
        );

        console.log("PASS: User A can update own profile");

        // Test 2: User A updates another user's profile
        await assertFails(
            updateDoc(doc(dbA, "users", "userB"), {
                name: "Hacked"
            })
        );

        console.log("PASS: User A cannot update another user's profile");

                // ========================================
                // ITEMS SECURITY RULES TEST
                // ========================================

                const dbB = testEnv.authenticatedContext("userB").firestore();

                const itemA = doc(dbA, "items", "item001");
                const itemB = doc(dbB, "items", "item001");

                // Test 3: User A creates own item
                await assertSucceeds(
                    setDoc(itemA, {
                        itemId: "item001",
                        ownerId: "userA",
                        itemName: "Drill",
                        description: "Electric drill",
                        category: "Tools",
                        price: 10,
                        location: "Sydney",
                        status: "ACTIVE"
                    })
                );

                console.log("PASS: Owner can create an item");

                // Test 4: Owner updates own item
                await assertSucceeds(
                    updateDoc(itemA, {
                        price: 15
                    })
                );

                console.log("PASS: Owner can update own item");

                // Test 5: Another user cannot update the item
                await assertFails(
                    updateDoc(itemB, {
                        price: 100
                    })
                );

                console.log("PASS: Other users cannot update the item");

                // Test 6: Owner cannot change ownership
                await assertFails(
                    updateDoc(itemA, {
                        ownerId: "userB"
                    })
                );

                console.log("PASS: Owner cannot change ownership");

                // Test 7: Owner marks item as PAST
                await assertSucceeds(
                    updateDoc(itemA, {
                        status: "PAST"
                    })
                );

                console.log("PASS: Owner can mark item as PAST");

                // Test 8: PAST item cannot be reactivated
                await assertFails(
                    updateDoc(itemA, {
                        status: "ACTIVE"
                    })
                );

                console.log("PASS: PAST item cannot be reactivated");

        // ========================================
        // NOTIFICATIONS SECURITY RULES TEST
        // ========================================

        // Prepare test data without security restrictions
        await testEnv.withSecurityRulesDisabled(async context => {

            const adminDb = context.firestore();

            // Create an ACTIVE item owned by User A
            await setDoc(doc(adminDb, "items", "notificationItem001"), {
                itemId: "notificationItem001",
                ownerId: "userA",
                itemName: "Camera",
                description: "Camera for rent",
                category: "Electronics",
                price: 20,
                location: "Sydney",
                status: "ACTIVE"
            });

            // Create a PENDING booking by User B
            await setDoc(doc(adminDb, "bookings", "notificationBooking001"), {
                bookingId: "notificationBooking001",
                borrowerId: "userB",
                itemId: "notificationItem001",
                startTime: 1000,
                endTime: 2000,
                status: "PENDING"
            });
        });

        const notificationA = doc(
            dbA,
            "notifications",
            "notification001"
        );

        const notificationB = doc(
            dbB,
            "notifications",
            "notification001"
        );

        const notificationData = {
            notificationId: "notification001",
            recipientId: "userA",
            senderId: "userB",
            type: "BOOKING_REQUEST",
            title: "New Booking Request",
            message: "User B requested your camera",
            relatedId: "notificationBooking001",
            isRead: false,
            createdAt: Date.now()
        };

        // Test 9: Borrower creates a valid booking notification
        await assertSucceeds(
            setDoc(notificationB, notificationData)
        );

        console.log("PASS: Borrower can create a valid notification");

        // Test 10: Owner can read received notification
        await assertSucceeds(
            getDoc(notificationA)
        );

        console.log("PASS: Recipient can read notification");

        // Test 11: Sender cannot read recipient's notification
        await assertFails(
            getDoc(notificationB)
        );

        console.log("PASS: Sender cannot read recipient's notification");

        // Test 12: Recipient can mark notification as read
        await assertSucceeds(
            updateDoc(notificationA, {
                isRead: true
            })
        );

        console.log("PASS: Recipient can mark notification as read");

        // Test 13: Sender cannot change read status
        await assertFails(
            updateDoc(notificationB, {
                isRead: false
            })
        );

        console.log("PASS: Sender cannot change read status");

        // Test 14: Recipient cannot change notification message
        await assertFails(
            updateDoc(notificationA, {
                message: "Modified message"
            })
        );

        console.log("PASS: Recipient cannot modify notification content");

        // Test 15: Recipient cannot delete notification
        await assertFails(
            deleteDoc(notificationA)
        );

        console.log("PASS: Recipient cannot delete notification");

        // Test 16: Another user cannot impersonate the borrower
        await assertFails(
            setDoc(doc(dbA, "notifications", "notification002"), {
                ...notificationData,
                notificationId: "notification002"
            })
        );

        console.log("PASS: Other users cannot impersonate borrower");

        // ========================================
        // REVIEWS SECURITY RULES TEST
        // ========================================

        // Prepare completed and pending bookings
        await testEnv.withSecurityRulesDisabled(async context => {

            const adminDb = context.firestore();

            // Item owned by User A
            await setDoc(doc(adminDb, "items", "reviewItem001"), {
                itemId: "reviewItem001",
                ownerId: "userA",
                itemName: "Camera",
                description: "Camera for rent",
                category: "Electronics",
                price: 20,
                location: "Sydney",
                status: "ACTIVE"
            });

            // Completed booking: User B borrowed from User A
            await setDoc(doc(adminDb, "bookings", "reviewBooking001"), {
                bookingId: "reviewBooking001",
                borrowerId: "userB",
                itemId: "reviewItem001",
                startTime: 1000,
                endTime: 2000,
                status: "COMPLETED"
            });

            // Pending booking: should not allow reviews
            await setDoc(doc(adminDb, "bookings", "reviewBooking002"), {
                bookingId: "reviewBooking002",
                borrowerId: "userB",
                itemId: "reviewItem001",
                startTime: 3000,
                endTime: 4000,
                status: "PENDING"
            });
        });

        const dbC = testEnv.authenticatedContext("userC").firestore();

        const borrowerReview = {
            reviewId: "reviewBooking001_userB",
            bookingId: "reviewBooking001",
            reviewerId: "userB",
            revieweeId: "userA",
            revieweeRole: "OWNER",
            rating: 5,
            comment: "Excellent owner!",
            createdAt: Date.now()
        };

        const ownerReview = {
            reviewId: "reviewBooking001_userA",
            bookingId: "reviewBooking001",
            reviewerId: "userA",
            revieweeId: "userB",
            revieweeRole: "BORROWER",
            rating: 4,
            comment: "Responsible borrower!",
            createdAt: Date.now()
        };

        // Test 17: Borrower reviews Owner
        await assertSucceeds(
            setDoc(
                doc(dbB, "reviews", "reviewBooking001_userB"),
                borrowerReview
            )
        );

        console.log("PASS: Borrower can review Owner");

        // Test 18: Owner reviews Borrower
        await assertSucceeds(
            setDoc(
                doc(dbA, "reviews", "reviewBooking001_userA"),
                ownerReview
            )
        );

        console.log("PASS: Owner can review Borrower");

        // Test 19: Duplicate review cannot overwrite existing review
        await assertFails(
            setDoc(
                doc(dbB, "reviews", "reviewBooking001_userB"),
                {
                    ...borrowerReview,
                    rating: 1
                }
            )
        );

        console.log("PASS: Duplicate review is rejected");

        // Test 20: Third party cannot impersonate Borrower
        await assertFails(
            setDoc(
                doc(dbC, "reviews", "reviewBooking001_userC"),
                {
                    ...borrowerReview,
                    reviewId: "reviewBooking001_userC",
                    reviewerId: "userC"
                }
            )
        );

        console.log("PASS: Third party cannot create review");

        // Test 21: Borrower cannot review themselves
        await assertFails(
            setDoc(
                doc(dbB, "reviews", "reviewBooking001_self"),
                {
                    ...borrowerReview,
                    reviewId: "reviewBooking001_self",
                    revieweeId: "userB"
                }
            )
        );

        console.log("PASS: Borrower cannot review themselves");

        // Test 22: Rating must be between 1 and 5
        await assertFails(
            setDoc(
                doc(dbB, "reviews", "reviewBooking001_invalid"),
                {
                    ...borrowerReview,
                    reviewId: "reviewBooking001_invalid",
                    rating: 6
                }
            )
        );

        console.log("PASS: Invalid rating is rejected");

        // Test 23: Pending booking cannot be reviewed
        await assertFails(
            setDoc(
                doc(dbB, "reviews", "reviewBooking002_userB"),
                {
                    ...borrowerReview,
                    reviewId: "reviewBooking002_userB",
                    bookingId: "reviewBooking002"
                }
            )
        );

        console.log("PASS: Pending booking cannot be reviewed");

        // Test 24: Submitted review cannot be modified
        await assertFails(
            updateDoc(
                doc(dbB, "reviews", "reviewBooking001_userB"),
                {
                    rating: 1
                }
            )
        );

        console.log("PASS: Submitted review cannot be modified");

        // Test 25: Submitted review cannot be deleted
        await assertFails(
            deleteDoc(
                doc(dbB, "reviews", "reviewBooking001_userB")
            )
        );

        console.log("PASS: Submitted review cannot be deleted");

        // -----------------------------------------------------
        // BOOKINGS SECURITY RULES TESTS
        // -----------------------------------------------------

        const bookingA = doc(dbA, "bookings", "booking1");

        // Test 1: Owner can accept pending booking
        await assertSucceeds(
            updateDoc(bookingA, {
                status: "ACCEPTED"
            })
        );

        console.log("PASS: Owner can accept pending booking");

        // Test 2: Borrower cannot cancel accepted booking

        await assertFails(
            updateDoc(doc(dbB, "bookings", "booking1"), {
                status: "CANCELLED"
            })
        );

        console.log("PASS: Borrower cannot cancel accepted booking");

        // Test 3: Borrower cannot accept booking
        await assertFails(
            updateDoc(doc(dbB, "bookings", "booking1"), {
                status: "REJECTED"
            })
        );

        console.log("PASS: Borrower cannot reject booking");

        // Test 4: Owner cannot change borrowerId
        await assertFails(
            updateDoc(bookingA, {
                borrowerId: "userA"
            })
        );

        console.log("PASS: Owner cannot change borrowerId");

        // Test 5: Owner cannot change itemId
        await assertFails(
            updateDoc(bookingA, {
                itemId: "anotherItem"
            })
        );

        console.log("PASS: Owner cannot change itemId");

        // Test 6: Borrower can cancel a pending booking

        // Create another pending booking without security restrictions
        await testEnv.withSecurityRulesDisabled(async context => {

            const adminDb = context.firestore();

            await setDoc(doc(adminDb, "bookings", "booking2"), {
                bookingId: "booking2",
                borrowerId: "userB",
                itemId: "bookingItem1",
                startTime: 3000,
                endTime: 4000,
                status: "PENDING"
            });

        });

        // Borrower cancels their own pending booking
        await assertSucceeds(
            updateDoc(doc(dbB, "bookings", "booking2"), {
                status: "CANCELLED"
            })
        );

        console.log("PASS: Borrower can cancel pending booking");

    } finally {
        await testEnv.cleanup();
    }
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});