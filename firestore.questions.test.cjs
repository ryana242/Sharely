const { test, after } = require('node:test');
const assert = require('node:assert/strict');

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');

const {
  doc,
  setDoc,
  updateDoc,
  getDoc,
  collection,
  query,
  where,
  orderBy,
  getDocs
} = require('firebase/firestore');

const fs = require('node:fs');

const PROJECT_ID = 'sharely-questions-test';

let testEnv;

async function setup() {
  if (!testEnv) {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8')
      }
    });
  }

  await testEnv.clearFirestore();

  await testEnv.withSecurityRulesDisabled(async context => {
    const db = context.firestore();

    await setDoc(doc(db, 'items', 'item1'), {
      itemId: 'item1',
      ownerId: 'owner1',
      status: 'ACTIVE'
    });
  });

  return {
    owner: testEnv.authenticatedContext('owner1').firestore(),
    borrower: testEnv.authenticatedContext('borrower1').firestore(),
    stranger: testEnv.authenticatedContext('stranger1').firestore()
  };
}

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

test('Borrower can create a public question', async () => {
  const { borrower } = await setup();

  await assertSucceeds(
    setDoc(doc(borrower, 'itemQuestions', 'q1'), {
      questionId: 'q1',
      itemId: 'item1',
      authorId: 'borrower1',
      question: 'Does this item include a charger?',
      answer: '',
      createdAt: Date.now()
    })
  );
});

test('Owner cannot ask a question on own item', async () => {
  const { owner } = await setup();

  await assertFails(
    setDoc(doc(owner, 'itemQuestions', 'q1'), {
      questionId: 'q1',
      itemId: 'item1',
      authorId: 'owner1',
      question: 'My own question?',
      answer: '',
      createdAt: Date.now()
    })
  );
});

test('Only owner can answer a question', async () => {
  const { owner, borrower, stranger } = await setup();

  await assertSucceeds(
    setDoc(doc(borrower, 'itemQuestions', 'q1'), {
      questionId: 'q1',
      itemId: 'item1',
      authorId: 'borrower1',
      question: 'Does it include a charger?',
      answer: '',
      createdAt: Date.now()
    })
  );

  await assertFails(
    updateDoc(doc(stranger, 'itemQuestions', 'q1'), {
      answer: 'Fake answer'
    })
  );

  await assertSucceeds(
    updateDoc(doc(owner, 'itemQuestions', 'q1'), {
      answer: 'Yes, it does.'
    })
  );
});

test('Users cannot modify existing questions', async () => {
  const { borrower } = await setup();

  await assertSucceeds(
    setDoc(doc(borrower, 'itemQuestions', 'q1'), {
      questionId: 'q1',
      itemId: 'item1',
      authorId: 'borrower1',
      question: 'Does it include a charger?',
      answer: '',
      createdAt: Date.now()
    })
  );

  await assertFails(
    updateDoc(doc(borrower, 'itemQuestions', 'q1'), {
      question: 'Changed question'
    })
  );
});

test('Signed-in users can read public questions', async () => {
  const { borrower, stranger } = await setup();

  await assertSucceeds(
    setDoc(doc(borrower, 'itemQuestions', 'q1'), {
      questionId: 'q1',
      itemId: 'item1',
      authorId: 'borrower1',
      question: 'Does it include a charger?',
      answer: '',
      createdAt: Date.now()
    })
  );

  await assertSucceeds(
    getDoc(doc(stranger, 'itemQuestions', 'q1'))
  );
});

test('Android question query is permitted', async () => {
  const { borrower, stranger } = await setup();

  await assertSucceeds(
    setDoc(doc(borrower, 'itemQuestions', 'q1'), {
      questionId: 'q1',
      itemId: 'item1',
      authorId: 'borrower1',
      question: 'Does it include a charger?',
      answer: '',
      createdAt: Date.now()
    })
  );

  await assertSucceeds(
    getDocs(
      query(
        collection(stranger, 'itemQuestions'),
        where('itemId', '==', 'item1'),
        orderBy('createdAt', 'asc')
      )
    )
  );
});