const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const KIRIONCE_UID = "kirionce_uid_01";
const OTHER_UID = "other_user_uid_02";

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

test("Unauthenticated user cannot read or write preferences or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(KIRIONCE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(KIRIONCE_UID).set({
      userId: KIRIONCE_UID,
      displayName: "Administrador Kirionce",
      affectionateMode: true,
      autoVoiceReply: true,
      selectedModel: "gemini-3.8-flash",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(KIRIONCE_UID).collection("messages").get()
  );
});

test("Owner can create and partially update UserPreferences; shadow fields and createdAt mutation rejected", async () => {
  const db = testEnv.authenticatedContext(KIRIONCE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    db.collection("users").doc(KIRIONCE_UID).set({
      userId: KIRIONCE_UID,
      displayName: "Administrador Kirionce",
      affectionateMode: true,
      autoVoiceReply: true,
      selectedModel: "gemini-3.8-flash",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update of mutable fields succeeds
  await assertSucceeds(
    db.collection("users").doc(KIRIONCE_UID).update({
      selectedModel: "gemini-4-argon",
      affectionateMode: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mutating immutable createdAt fails
  await assertFails(
    db.collection("users").doc(KIRIONCE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost / shadow field fails
  await assertFails(
    db.collection("users").doc(KIRIONCE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid model enum fails
  await assertFails(
    db.collection("users").doc(KIRIONCE_UID).update({
      selectedModel: "gemini-1.5-pro",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create, list, and delete messages in ALICE, YUI, and GROUP channels; updates rejected", async () => {
  const db = testEnv.authenticatedContext(KIRIONCE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const messagesRef = db.collection("users").doc(KIRIONCE_UID).collection("messages");

  // Empty collection list succeeds
  await assertSucceeds(messagesRef.where("channelId", "==", "ALICE").get());

  // Valid create payload (no @DocumentId 'id' field)
  await assertSucceeds(
    messagesRef.doc("msg_001").set({
      userId: KIRIONCE_UID,
      channelId: "ALICE",
      speaker: "USER",
      content: "Hola Alice, Administrador Kirionce aquí.",
      errorFlag: false,
      sequenceOrder: 1700000000001,
      createdAt: now,
    })
  );

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(messagesRef.where("channelId", "==", "ALICE").get());

  // Extra @DocumentId 'id' field fails
  await assertFails(
    messagesRef.doc("msg_002").set({
      id: "msg_002",
      userId: KIRIONCE_UID,
      channelId: "YUI",
      speaker: "YUI",
      content: "Hola!",
      errorFlag: false,
      sequenceOrder: 1700000000002,
      createdAt: now,
    })
  );

  // Future timestamp fails
  await assertFails(
    messagesRef.doc("msg_future").set({
      userId: KIRIONCE_UID,
      channelId: "GROUP",
      speaker: "ALICE",
      content: "Timestamp futuro",
      errorFlag: false,
      sequenceOrder: 1700000000003,
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Updates to immutable chat messages are rejected
  await assertFails(messagesRef.doc("msg_001").update({ content: "Editado" }));

  // Owner can delete their own message
  await assertSucceeds(messagesRef.doc("msg_001").delete());
});

test("Cross-user isolation: another authenticated user cannot read, write, or delete Kirionce's data", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(KIRIONCE_UID).set({
      userId: KIRIONCE_UID,
      displayName: "Administrador Kirionce",
      affectionateMode: true,
      autoVoiceReply: true,
      selectedModel: "gemini-3.8-flash",
      createdAt: created,
      updatedAt: created,
    });
    await adminDb
      .collection("users")
      .doc(KIRIONCE_UID)
      .collection("messages")
      .doc("private_msg")
      .set({
        userId: KIRIONCE_UID,
        channelId: "ALICE",
        speaker: "ALICE",
        content: "Mensaje privado",
        errorFlag: false,
        sequenceOrder: 1700000000010,
        createdAt: created,
      });
  });

  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  await assertFails(otherDb.collection("users").doc(KIRIONCE_UID).get());
  await assertFails(
    otherDb.collection("users").doc(KIRIONCE_UID).collection("messages").get()
  );
  await assertFails(
    otherDb
      .collection("users")
      .doc(KIRIONCE_UID)
      .collection("messages")
      .doc("private_msg")
      .get()
  );
  await assertFails(
    otherDb
      .collection("users")
      .doc(KIRIONCE_UID)
      .collection("messages")
      .doc("private_msg")
      .delete()
  );
});
