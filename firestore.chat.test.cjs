
const fs = require('fs');

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} = require('@firebase/rules-unit-testing');

const {
  doc,
  setDoc,
  getDoc,
  updateDoc,
  getDocs,
  collection,
  query,
  where,
  orderBy,
  writeBatch
} = require('firebase/firestore');

const projectId = 'sharely-7205c';
const CONV = 'test-conversation';
const ITEM = 'chat-item';

let passed = 0;
let failed = 0;

function conversation(now) {
  return {
    conversationId: CONV,
    itemId: ITEM,
    itemName: 'Camera',
    ownerId: 'owner',
    borrowerId: 'borrower',
    participantIds: ['owner', 'borrower'],
    createdAt: now,
    updatedAt: now,
    lastMessage: '',
    lastSenderId: ''
  };
}

async function check(name, fn) {
  try {
    await fn();
    passed++;
    console.log('PASS:', name);
  } catch (e) {
    failed++;
    console.error('FAIL:', name);
    console.error('Reason:', e.message);
  }
}

async function main() {
  const env = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: '127.0.0.1',
      port: 8080,
      rules: fs.readFileSync('firestore.rules', 'utf8')
    }
  });

  try {
    const owner =
      env.authenticatedContext('owner').firestore();

    const borrower =
      env.authenticatedContext('borrower').firestore();

    const stranger =
      env.authenticatedContext('stranger').firestore();

    const now = Date.now();

    // Create test listing without security restrictions.
    await env.withSecurityRulesDisabled(async ctx => {
      await setDoc(
        doc(ctx.firestore(), 'items', ITEM),
        {
          itemId: ITEM,
          ownerId: 'owner',
          itemName: 'Camera',
          status: 'ACTIVE'
        }
      );
    });

    await check('Borrower creates a conversation', async () => {
      await assertSucceeds(
        setDoc(
          doc(borrower, 'conversations', CONV),
          conversation(now)
        )
      );
    });

    await check('Owner can read conversation', async () => {
      await assertSucceeds(
        getDoc(doc(owner, 'conversations', CONV))
      );
    });

    await check('Stranger cannot read conversation', async () => {
      await assertFails(
        getDoc(doc(stranger, 'conversations', CONV))
      );
    });

    await check('Borrower inbox query works', async () => {
      await assertSucceeds(
        getDocs(
          query(
            collection(borrower, 'conversations'),
            where(
              'participantIds',
              'array-contains',
              'borrower'
            )
          )
        )
      );
    });

    await check('Owner inbox query works', async () => {
      await assertSucceeds(
        getDocs(
          query(
            collection(owner, 'conversations'),
            where(
              'participantIds',
              'array-contains',
              'owner'
            )
          )
        )
      );
    });

    await check('Stranger cannot impersonate borrower', async () => {
      const other = {
        ...conversation(now),
        conversationId: 'attacker'
      };

      await assertFails(
        setDoc(
          doc(stranger, 'conversations', 'attacker'),
          other
        )
      );
    });

    await check('No one can change conversation participants', async () => {
      await assertFails(
        updateDoc(
          doc(borrower, 'conversations', CONV),
          {
            participantIds: ['owner', 'stranger']
          }
        )
      );
    });

    await check(
      'Cannot send message without updating conversation',
      async () => {
        await assertFails(
          setDoc(
            doc(
              borrower,
              'conversations',
              CONV,
              'messages',
              'unpaired'
            ),
            {
              messageId: 'unpaired',
              senderId: 'borrower',
              recipientId: 'owner',
              text: 'Hello',
              createdAt: now + 100
            }
          )
        );
      }
    );

    await check('Borrower sends paired message', async () => {
      const batch = writeBatch(borrower);
      const at = now + 500;

      batch.set(
        doc(
          borrower,
          'conversations',
          CONV,
          'messages',
          'message1'
        ),
        {
          messageId: 'message1',
          senderId: 'borrower',
          recipientId: 'owner',
          text: 'Hello',
          createdAt: at
        }
      );

      batch.update(
        doc(borrower, 'conversations', CONV),
        {
          lastMessage: 'Hello',
          lastSenderId: 'borrower',
          updatedAt: at
        }
      );

      await assertSucceeds(batch.commit());
    });

    await check(
      'Owner can receive messages in ordered query',
      async () => {
        await assertSucceeds(
          getDocs(
            query(
              collection(
                owner,
                'conversations',
                CONV,
                'messages'
              ),
              orderBy('createdAt')
            )
          )
        );
      }
    );

    await check('Stranger cannot read messages', async () => {
      await assertFails(
        getDoc(
          doc(
            stranger,
            'conversations',
            CONV,
            'messages',
            'message1'
          )
        )
      );
    });

    await check('Stranger cannot send messages', async () => {
      await assertFails(
        setDoc(
          doc(
            stranger,
            'conversations',
            CONV,
            'messages',
            'attack'
          ),
          {
            messageId: 'attack',
            senderId: 'stranger',
            recipientId: 'owner',
            text: 'Hi',
            createdAt: now + 600
          }
        )
      );
    });

    await check(
      'Messages cannot be changed after sending',
      async () => {
        await assertFails(
          updateDoc(
            doc(
              borrower,
              'conversations',
              CONV,
              'messages',
              'message1'
            ),
            {
              text: 'Tampered'
            }
          )
        );
      }
    );

    await check('New-message notification works', async () => {
      await assertSucceeds(
        setDoc(
          doc(borrower, 'notifications', 'chat-notice'),
          {
            notificationId: 'chat-notice',
            recipientId: 'owner',
            senderId: 'borrower',
            type: 'NEW_MESSAGE',
            title: 'New Message',
            message: 'You have received a new message.',
            relatedId: CONV,
            isRead: false,
            createdAt: now + 550
          }
        )
      );
    });

    await check(
      'Outsider cannot forge chat notification',
      async () => {
        await assertFails(
          setDoc(
            doc(stranger, 'notifications', 'fake-notice'),
            {
              notificationId: 'fake-notice',
              recipientId: 'owner',
              senderId: 'stranger',
              type: 'NEW_MESSAGE',
              title: 'New Message',
              message: 'Fake',
              relatedId: CONV,
              isRead: false,
              createdAt: now + 600
            }
          )
        );
      }
    );

    console.log('\n==============================');
    console.log('CHAT SECURITY TEST RESULTS');
    console.log('==============================');

    console.log('Passed:', passed);
    console.log('Failed:', failed);
    console.log('Total:', passed + failed);

    if (failed > 0) {
      process.exitCode = 1;
    }
  } finally {
    await env.cleanup();
  }
}

main().catch(error => {
  console.error('Test suite error:', error);
  process.exitCode = 1;
});
