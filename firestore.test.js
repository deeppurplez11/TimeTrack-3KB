const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const WORKSPACE_ID = "3kb_main";
const ALICE_UID = "alice_email1_uid";
const BOB_UID = "bob_email2_uid";
const CHARLIE_UID = "charlie_nonmember_uid";
const ADMIN_UID = "admin_ceo_uid";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

async function bootstrapWorkspaceAndMember(db, uid, ts) {
  const wsRef = db.collection("workspaces").doc(WORKSPACE_ID);
  const wsSnap = await wsRef.get();
  if (!wsSnap.exists) {
    await wsRef.set({
      ownerUid: uid,
      workspaceId: WORKSPACE_ID,
      name: "374 Kingdom Builders",
      createdAt: ts,
      updatedAt: ts,
    });
  }
  await db.collection(`workspaces/${WORKSPACE_ID}/members`).doc(uid).set({
    ownerUid: uid,
    workspaceId: WORKSPACE_ID,
    role: "member",
    createdAt: ts,
    updatedAt: ts,
  });
}

async function submitDevotionalPair(
  db,
  uid,
  empUid,
  empId,
  empName,
  dateKey,
  ts,
  shareWithAdmin = false,
  photoUrl = "https://firebasestorage.googleapis.com/example/proof.jpg"
) {
  const devId = `${empUid}_${dateKey}`;
  const batch = db.batch();
  batch.set(db.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId), {
    ownerUid: uid,
    workspaceId: WORKSPACE_ID,
    employeeUid: empUid,
    employeeId: empId,
    employeeName: empName,
    dateKey,
    scripture: "Proverbs 16:3 — Commit to the Lord whatever you do.",
    observation: "Dedicated work brings clarity and peace.",
    application: "Start today with prayer and diligence.",
    prayer: "Lord, guide our team today.",
    photoStoragePath: `workspaces/${WORKSPACE_ID}/devotionals/${empUid}/${dateKey}/proof.jpg`,
    photoUrl,
    status: "SUBMITTED",
    shareWithAdmin,
    submittedAt: ts,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(db.collection(`workspaces/${WORKSPACE_ID}/devotional_status`).doc(devId), {
    ownerUid: uid,
    workspaceId: WORKSPACE_ID,
    employeeUid: empUid,
    employeeId: empId,
    employeeName: empName,
    dateKey,
    status: "SUBMITTED",
    hasPhoto: true,
    shareWithAdmin,
    submittedAt: ts,
    createdAt: ts,
    updatedAt: ts,
  });
  return batch.commit();
}

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

test("Unauthenticated user cannot read or write workspace, members, employees, attendance, breaks, or devotionals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("workspaces").where("workspaceId", "==", WORKSPACE_ID).get());
  await assertFails(
    unauthDb.collection(`workspaces/${WORKSPACE_ID}/employees`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  await assertFails(
    unauthDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  await assertFails(
    unauthDb.collection(`workspaces/${WORKSPACE_ID}/breaks`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  await assertFails(
    unauthDb.collection(`workspaces/${WORKSPACE_ID}/devotional_status`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  await assertFails(
    unauthDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  await assertFails(
    unauthDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_1").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-001",
      fullName: "Marco Santos",
      role: "Engineer",
      pin: "1234",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 1000,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Any two Google accounts (email1 Alice and email2 Bob) share workspace and Clock In requires submitted devotional", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const dateKey = "2026-11-24";
  const empUid = "emp_3kb_001";
  const attId = `${empUid}_${dateKey}`;

  // Day 1: Alice signs in with email1, bootstraps workspace + membership, and creates data
  await assertSucceeds(bootstrapWorkspaceAndMember(aliceDb, ALICE_UID, ts));

  // Empty collection filtered query succeeds (matches Kotlin observeEmployees)
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).where("workspaceId", "==", WORKSPACE_ID).get()
  );

  // Alice creates an employee (no @DocumentId field)
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc(empUid).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-001",
      fullName: "Marco Santos",
      role: "Lead Engineer",
      pin: "1234",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 10000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Backend gate: Clock In MUST fail if devotional has NOT been submitted yet
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(attId).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeUid: empUid,
      devotionalId: attId,
      dateKey,
      clockInMillis: Date.now() - 3600000,
      status: "WORKING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Submit devotional + devotional_status atomically
  await assertSucceeds(
    submitDevotionalPair(aliceDb, ALICE_UID, empUid, "3KB-001", "Marco Santos", dateKey, ts, true)
  );

  // Now Clock In succeeds with linked devotionalId
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(attId).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeUid: empUid,
      devotionalId: attId,
      dateKey,
      clockInMillis: Date.now() - 3600000,
      status: "WORKING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/breaks`).doc("brk_1").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      attendanceId: attId,
      employeeUid: empUid,
      dateKey,
      breakStartMillis: Date.now() - 1200000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(attId).update({
      status: "ON_BREAK",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Day 2: Bob signs in with email2, joins the shared workspace, and sees Alice's shared operational data
  await assertSucceeds(bootstrapWorkspaceAndMember(bobDb, BOB_UID, new Date(Date.now() - 900)));

  const bobEmps = await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/employees`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  assert.equal(bobEmps.size, 1);
  assert.equal(bobEmps.docs[0].data().fullName, "Marco Santos");

  const bobDevStatuses = await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/devotional_status`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  assert.equal(bobDevStatuses.size, 1);

  const bobAtt = await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  assert.equal(bobAtt.size, 1);

  // Bob can update employee info created by Alice
  await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc(empUid).update({
      employeeId: "3KB-001",
      fullName: "Marco Santos Jr.",
      role: "Principal Engineer",
      updatedAt: new Date(Date.now() - 600),
    })
  );

  // Bob can end the break and clock out the attendance created by Alice
  await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/breaks`).doc("brk_1").update({
      breakEndMillis: Date.now() - 400,
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(attId).update({
      status: "WORKING",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertSucceeds(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(attId).update({
      clockOutMillis: Date.now() - 100,
      status: "CLOCKED_OUT",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Terminal state locking: neither Alice nor Bob can mutate CLOCKED_OUT record
  await assertFails(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(attId).update({
      status: "WORKING",
      updatedAt: new Date(Date.now() - 50),
    })
  );
});

test("Devotional privacy isolation, admin verification, atomic batch guard, and attendance reset preservation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const verifiedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "374kingdombuilders@gmail.com",
    email_verified: true,
  }).firestore();
  const unverifiedSpoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "374kingdombuilders@gmail.com",
    email_verified: false,
  }).firestore();

  const ts = new Date(Date.now() - 5000);
  const dateKey = "2026-11-25";
  const empUid = "emp_privacy_01";
  const devId = `${empUid}_${dateKey}`;

  await assertSucceeds(bootstrapWorkspaceAndMember(aliceDb, ALICE_UID, ts));
  await assertSucceeds(bootstrapWorkspaceAndMember(bobDb, BOB_UID, ts));
  await assertSucceeds(bootstrapWorkspaceAndMember(verifiedAdminDb, ADMIN_UID, ts));
  await assertSucceeds(bootstrapWorkspaceAndMember(unverifiedSpoofDb, "spoof_uid", ts));

  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc(empUid).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-010",
      fullName: "Camille Bautista",
      role: "QA",
      pin: "4567",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Creating devotional without matching devotional_status MUST fail
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeUid: empUid,
      employeeId: "3KB-010",
      employeeName: "Camille Bautista",
      dateKey,
      scripture: "Psalm 23:1",
      observation: "God is our shepherd.",
      application: "Trust Him today.",
      prayer: "Lord, lead me.",
      photoStoragePath: `workspaces/${WORKSPACE_ID}/devotionals/${empUid}/${dateKey}/p.jpg`,
      photoUrl: "https://example.com/p.jpg",
      status: "SUBMITTED",
      shareWithAdmin: false,
      submittedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Atomic batch succeeds
  await assertSucceeds(
    submitDevotionalPair(aliceDb, ALICE_UID, empUid, "3KB-010", "Camille Bautista", dateKey, ts, false)
  );

  // Alice can read and list her own devotional
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).get()
  );
  const aliceOwnList = await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).where("ownerUid", "==", ALICE_UID).get()
  );
  assert.equal(aliceOwnList.size, 1);

  // Bob CANNOT get Alice's devotional OR list all workspace devotionals OR filter by Alice's employeeUid
  await assertFails(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).get()
  );
  await assertFails(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  await assertFails(
    bobDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).where("employeeUid", "==", empUid).get()
  );

  // Unverified spoofed email CANNOT read/list private devotionals
  await assertFails(
    unverifiedSpoofDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).get()
  );
  await assertFails(
    unverifiedSpoofDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).where("workspaceId", "==", WORKSPACE_ID).get()
  );

  // Verified Admin CAN read & list devotionals
  await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).get()
  );
  const adminList = await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).where("workspaceId", "==", WORKSPACE_ID).get()
  );
  assert.equal(adminList.size, 1);

  // Create attendance linked to devotional, then delete (reset) attendance -> devotional remains immutable & undeletable
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(devId).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeUid: empUid,
      devotionalId: devId,
      dateKey,
      clockInMillis: Date.now() - 2000,
      status: "WORKING",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc(devId).delete()
  );
  await assertFails(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).delete()
  );
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotional_status`).doc(devId).delete()
  );
  const devStillExists = await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).get()
  );
  assert.equal(devStillExists.exists, true);

  // Privacy settings validation
  await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/settings`).doc("devotional_privacy").set({
      ownerUid: ADMIN_UID,
      workspaceId: WORKSPACE_ID,
      policy: "OPT_IN_ONLY",
      updatedByUid: ADMIN_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/settings`).doc("devotional_privacy").update({
      policy: "INVALID_POLICY",
      updatedByUid: ADMIN_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Compact inline fallback data-URI succeeds while oversized > 350KB fails
  const empInlineUid = "emp_inline_02";
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc(empInlineUid).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-011",
      fullName: "Hannah Cruz",
      role: "Ops",
      pin: "7890",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  const compactDataUri = "data:image/jpeg;base64," + "A".repeat(4096);
  await assertSucceeds(
    submitDevotionalPair(aliceDb, ALICE_UID, empInlineUid, "3KB-011", "Hannah Cruz", dateKey, ts, true, compactDataUri)
  );
  const oversizedDataUri = "data:image/jpeg;base64," + "A".repeat(360000);
  await assertFails(
    submitDevotionalPair(aliceDb, ALICE_UID, empInlineUid, "3KB-011", "Hannah Cruz", "2026-11-26", ts, true, oversizedDataUri)
  );

  // Deleting an employee from Team allows cleaning up their devotional_status & devotionals
  await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc(empUid).delete()
  );
  await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/devotional_status`).doc(devId).delete()
  );
  await assertSucceeds(
    verifiedAdminDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).doc(devId).delete()
  );
});

test("Unfiltered queries, non-members, other workspaces, identity spoofing, ghost fields, and invalid schemas fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(bootstrapWorkspaceAndMember(aliceDb, ALICE_UID, ts));
  await assertSucceeds(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_1").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-001",
      fullName: "Marco Santos",
      role: "Engineer",
      pin: "1234",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unfiltered list queries MUST be rejected by allow list
  await assertFails(aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).get());
  await assertFails(aliceDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).get());
  await assertFails(aliceDb.collection(`workspaces/${WORKSPACE_ID}/breaks`).get());
  await assertFails(aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotionals`).get());
  await assertFails(aliceDb.collection(`workspaces/${WORKSPACE_ID}/devotional_status`).get());

  // Other workspace ID MUST be rejected
  await assertFails(
    aliceDb.collection("workspaces").doc("other_ws").set({
      ownerUid: ALICE_UID,
      workspaceId: "other_ws",
      name: "Rogue Workspace",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-member Charlie (has not joined /members/{uid}) cannot get or write employee docs
  await assertFails(
    charlieDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_1").get()
  );
  await assertFails(
    charlieDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_rogue").set({
      ownerUid: CHARLIE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-999",
      fullName: "Rogue User",
      role: "Dev",
      pin: "9999",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Charlie cannot spoof Alice's membership doc or escalate role
  await assertFails(
    charlieDb.collection(`workspaces/${WORKSPACE_ID}/members`).doc(ALICE_UID).set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      role: "member",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    charlieDb.collection(`workspaces/${WORKSPACE_ID}/members`).doc(CHARLIE_UID).set({
      ownerUid: CHARLIE_UID,
      workspaceId: WORKSPACE_ID,
      role: "admin",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Immutable fields cannot be mutated on update
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_1").update({
      ownerUid: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_2").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-002",
      fullName: "Aira",
      role: "Dev",
      pin: "2345",
      active: true,
      admin: false,
      ghostField: true,
      createdAtMillis: Date.now() - 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject invalid PIN format
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_3").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-003",
      fullName: "Bad Pin",
      role: "Dev",
      pin: "abcd",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 5000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject orphaned attendance without existing employee or devotional
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/attendance`).doc("non_existent_emp_2026-11-24").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeUid: "non_existent_emp",
      devotionalId: "non_existent_emp_2026-11-24",
      dateKey: "2026-11-24",
      clockInMillis: Date.now() - 1000,
      status: "WORKING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection(`workspaces/${WORKSPACE_ID}/employees`).doc("emp_future").set({
      ownerUid: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      employeeId: "3KB-009",
      fullName: "Future User",
      role: "Dev",
      pin: "9999",
      active: true,
      admin: false,
      createdAtMillis: Date.now() - 5000,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});
