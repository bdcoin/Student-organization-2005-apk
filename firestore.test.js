const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_kalir_bazar";
const ALICE_UID = "alice_device_1";
const ALICE_DEVICE2_UID = "alice_device_2";
const BOB_UID = "bob_intruder";
const PIN_HASH = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";
const NEW_PIN_HASH = "b665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";
const HASH_64 = "c665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildMemberPayload(overrides = {}) {
  const now = new Date();
  return {
    memberId: "FZ-2005-001",
    encryptedName: "ENCv1:encryptedNamePayload",
    encryptedAddress: "ENCv1:encryptedAddressPayload",
    encryptedMobile: "ENCv1:encryptedMobilePayload",
    encryptedNid: "ENCv1:encryptedNidPayload",
    encryptedNomineeName: "ENCv1:encryptedNomineeNamePayload",
    encryptedNomineeAddress: "ENCv1:encryptedNomineeAddrPayload",
    encryptedNomineeNid: "ENCv1:encryptedNomineeNidPayload",
    pinHash: PIN_HASH,
    mobileLookupHash: HASH_64,
    memberIdLookupHash: HASH_64,
    lastBindProof: "init",
    totalDeposited: 5000,
    totalWithdrawn: 0,
    authorizedUids: [ALICE_UID],
    transactionIds: [],
    createdByUid: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function buildTransactionPayload(overrides = {}) {
  const now = new Date();
  return {
    memberDocId: "member_001",
    memberId: "FZ-2005-001",
    encryptedMemberName: "ENCv1:encryptedNamePayload",
    type: "DEPOSIT",
    amount: 5000,
    monthYear: "2026-11",
    transactionDate: 1794000000000,
    encryptedNote: "ENCv1:MonthlyInstallment",
    receiptNumber: "RCP-202611-001",
    authorizedUids: [ALICE_UID],
    createdByUid: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("members").get());
  await assertFails(unauthDb.collection("transactions").get());
  await assertFails(unauthDb.collection("admins").get());
});

test("Admin: can register admin profile and create member + transaction", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminCodeHash: HASH_64,
      displayName: "Kalir Bazar Admin",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("members").doc("member_001").set(buildMemberPayload())
  );

  await assertSucceeds(
    adminDb.collection("transactions").doc("tx_001").set(buildTransactionPayload())
  );
});

test("Non-admin user: cannot create member or transaction, and cannot read another member's PII", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("members").doc("member_001").set(buildMemberPayload());
    await db.collection("transactions").doc("tx_001").set(buildTransactionPayload());
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // PII Blanket Test: Bob cannot get Alice's member profile or transaction
  await assertFails(bobDb.collection("members").doc("member_001").get());
  await assertFails(bobDb.collection("transactions").doc("tx_001").get());
  // Bob cannot create a member or transaction
  await assertFails(
    bobDb.collection("members").doc("member_002").set(buildMemberPayload({ createdByUid: BOB_UID }))
  );
  await assertFails(
    bobDb.collection("transactions").doc("tx_002").set(buildTransactionPayload({ createdByUid: BOB_UID }))
  );
});

test("Authorized Member: can query own profile & transactions, change PIN, and bind second device with PIN proof", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("members").doc("member_001").set(buildMemberPayload());
    await db.collection("transactions").doc("tx_001").set(buildTransactionPayload());
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("members").where("authorizedUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("transactions").where("authorizedUids", "array-contains", ALICE_UID).get()
  );

  // Member cannot edit totalDeposited
  await assertFails(
    aliceDb.collection("members").doc("member_001").update({
      totalDeposited: 999999,
      updatedAt: new Date(),
    })
  );

  // Second device binds with valid PIN proof
  const device2Db = testEnv.authenticatedContext(ALICE_DEVICE2_UID).firestore();
  await assertFails(
    device2Db.collection("members").doc("member_001").update({
      authorizedUids: [ALICE_UID, ALICE_DEVICE2_UID],
      lastBindProof: `${ALICE_DEVICE2_UID}:wrong_hash_0000000000000000000000000000000000000000000000000000`,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    device2Db.collection("members").doc("member_001").update({
      authorizedUids: [ALICE_UID, ALICE_DEVICE2_UID],
      lastBindProof: `${ALICE_DEVICE2_UID}:${PIN_HASH}`,
      updatedAt: new Date(),
    })
  );

  // Now Alice can change her PIN
  await assertSucceeds(
    aliceDb.collection("members").doc("member_001").update({
      pinHash: NEW_PIN_HASH,
      lastBindProof: `${ALICE_UID}:${NEW_PIN_HASH}`,
      updatedAt: new Date(),
    })
  );
});

test("Shadow Update & Email Spoofing Tests: rejected by rules", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("members").doc("member_001").set(buildMemberPayload());
  });

  // Spoofed admin email with email_verified: false
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "dragonboy20002@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("members").doc("member_001").get());

  // Shadow field injection
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("members").doc("member_001").update({
      pinHash: NEW_PIN_HASH,
      isVerifiedGhostField: true,
      updatedAt: new Date(),
    })
  );
});
