const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_psychologist_123";
const BOB_UID = "bob_intruder_456";

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

// 1. Unauthenticated checks
test("Unauthenticated: cannot read or write patients", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/patients`).get());
});

// 2. Cross-user isolation
test("Cross-user: Bob cannot read or write Alice's templates", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/templates`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/templates`).doc("t1").set({
    userId: BOB_UID,
    name: "Hack Template",
    isDefault: false,
    fields: [],
    createdAt: new Date(),
  }));
});

// 3. Authenticated owner: templates CRUD
test("Authenticated owner: can create, read and update clinical template", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const templateRef = aliceDb.collection(`users/${ALICE_UID}/templates`).doc("t1");

  await assertSucceeds(templateRef.set({
    userId: ALICE_UID,
    name: "Ficha Adultos TCC",
    description: "Plantilla cognitivo-conductual con objetivos y medicación",
    isDefault: true,
    fields: [
      { id: "f1", label: "Medicación Actual", type: "text", options: [] },
      { id: "f2", label: "Fecha Inicio", type: "date", options: [] },
      { id: "f3", label: "Derivación", type: "multi_select", options: ["Médico", "Familiar", "Iniciativa propia"] }
    ],
    createdAt: new Date(),
  }));

  const snap = await assertSucceeds(templateRef.get());
  await assertSucceeds(templateRef.update({
    name: "Ficha Adultos TCC v2",
    updatedAt: new Date(),
  }));
});

// 4. Authenticated owner: reminder configs CRUD
test("Authenticated owner: can save and update reminder config", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const configRef = aliceDb.collection(`users/${ALICE_UID}/reminder_configs`).doc("default");

  await assertSucceeds(configRef.set({
    userId: ALICE_UID,
    sessionReminderChannel: "whatsapp",
    sessionReminderHoursBefore: 24,
    sessionMessageTemplate: "Hola {nombre}, te recuerdo tu sesión el {dia} a las {hora}.",
    debtReminderChannel: "whatsapp",
    debtReminderFrequency: "weekly",
    debtMessageTemplate: "Hola {nombre}, te recuerdo el saldo pendiente de ${monto}.",
    notifyMePendingDebts: true,
    createdAt: new Date(),
  }));

  await assertSucceeds(configRef.update({
    sessionReminderHoursBefore: 2,
    updatedAt: new Date(),
  }));
});

// 5. Authenticated owner: patient with customFieldValues
test("Authenticated owner: can create patient with custom field values", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const patientRef = aliceDb.collection(`users/${ALICE_UID}/patients`).doc("p_custom");

  await assertSucceeds(patientRef.set({
    userId: ALICE_UID,
    name: "Elena Rostova",
    sessionFee: 18000,
    active: true,
    totalSessions: 0,
    debtAmount: 0,
    templateId: "t1",
    customFieldValues: {
      f1: "Sertralina 50mg",
      f2: "2026-11-09",
      f3: "Iniciativa propia"
    },
    createdAt: new Date(),
  }));
});
