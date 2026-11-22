const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, ts) {
  return {
    userId: uid,
    displayName: "Rahim",
    age: 28,
    gender: "MALE",
    heightCm: 172.0,
    currentWeightKg: 76.5,
    targetWeightKg: 68.0,
    activityLevel: "MODERATELY_ACTIVE",
    goalType: "LOSS",
    weeklyRateKg: 0.5,
    bmr: 1689,
    tdee: 2618,
    dailyCalorieTarget: 2068,
    proteinTargetG: 155,
    carbsTargetG: 207,
    fatTargetG: 69,
    waterTargetMl: 2678,
    mealReminderEnabled: true,
    waterReminderEnabled: true,
    weightReminderEnabled: false,
    onboardingDone: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validFoodPayload(uid, ts) {
  return {
    userId: uid,
    dateKey: "2026-11-21",
    mealType: "LUNCH",
    name: "ভাত ও মাছের ঝোল",
    quantity: 2.0,
    unit: "কাপ",
    grams: 320.0,
    calories: 480,
    proteinG: 28.5,
    carbsG: 68.0,
    fatG: 9.5,
    fiberG: 3.2,
    vitaminAMcg: 45.0,
    vitaminCMg: 12.0,
    vitaminDMcg: 3.5,
    vitaminEMg: 1.8,
    vitaminB6Mg: 0.4,
    vitaminB12Mcg: 1.6,
    source: "AI_TEXT",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validExercisePayload(uid, ts) {
  return {
    userId: uid,
    dateKey: "2026-11-21",
    activityName: "সকালের দৌড়",
    category: "RUNNING",
    intensity: "MODERATE",
    durationMinutes: 30,
    caloriesBurned: 375,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validWaterPayload(uid, ts) {
  return {
    userId: uid,
    dateKey: "2026-11-21",
    amountMl: 1250,
    targetMl: 2700,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validWeightPayload(uid, ts) {
  return {
    userId: uid,
    dateKey: "2026-11-21",
    weightKg: 76.2,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validFavoritePayload(uid, ts) {
  return {
    userId: uid,
    name: "সিদ্ধ ডিম",
    defaultQuantity: 2.0,
    unit: "পিস",
    grams: 100.0,
    calories: 155,
    proteinG: 13.0,
    carbsG: 1.1,
    fatG: 11.0,
    fiberG: 0.0,
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("food_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("exercise_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("water_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("weight_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorite_foods").get());
});

test("Owner can create, read, partially update, and delete UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 10000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID, ts)));
  await assertSucceeds(profileRef.get());

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    profileRef.update({
      currentWeightKg: 75.8,
      dailyCalorieTarget: 2040,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification & ghost field
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can CRUD food_logs, exercise_logs, water_logs, weight_logs, and favorite_foods", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 10000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Food log
  const foodRef = userRef.collection("food_logs").doc("food_1");
  await assertSucceeds(foodRef.set(validFoodPayload(ALICE_UID, ts)));
  await assertSucceeds(userRef.collection("food_logs").get());
  await assertSucceeds(
    foodRef.update({
      quantity: 1.5,
      calories: 360,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(foodRef.delete());

  // Exercise log
  const exRef = userRef.collection("exercise_logs").doc("ex_1");
  await assertSucceeds(exRef.set(validExercisePayload(ALICE_UID, ts)));
  await assertSucceeds(userRef.collection("exercise_logs").get());
  await assertSucceeds(exRef.delete());

  // Water log keyed by dateKey
  const waterRef = userRef.collection("water_logs").doc("2026-11-21");
  await assertSucceeds(waterRef.set(validWaterPayload(ALICE_UID, ts)));
  await assertSucceeds(
    waterRef.update({
      amountMl: 1500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(userRef.collection("water_logs").get());

  // Weight log (without optional note)
  const weightRef = userRef.collection("weight_logs").doc("w_1");
  await assertSucceeds(weightRef.set(validWeightPayload(ALICE_UID, ts)));
  await assertSucceeds(userRef.collection("weight_logs").get());

  // Weight log (with optional note)
  const weightRef2 = userRef.collection("weight_logs").doc("w_2");
  await assertSucceeds(
    weightRef2.set({
      ...validWeightPayload(ALICE_UID, ts),
      note: "সকালে খালি পেটে",
    })
  );

  // Favorite food
  const favRef = userRef.collection("favorite_foods").doc("fav_1");
  await assertSucceeds(favRef.set(validFavoritePayload(ALICE_UID, ts)));
  await assertSucceeds(userRef.collection("favorite_foods").get());
  await assertSucceeds(favRef.delete());
});

test("Cross-user isolation prevents Bob from reading or writing Alice's data", async () => {
  const ts = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("food_logs").doc("f_1").set(validFoodPayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("exercise_logs").doc("e_1").set(validExercisePayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("water_logs").doc("2026-11-21").set(validWaterPayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("weight_logs").doc("w_1").set(validWeightPayload(ALICE_UID, ts));
    await db.collection("users").doc(ALICE_UID).collection("favorite_foods").doc("fav_1").set(validFavoritePayload(ALICE_UID, ts));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRef = bobDb.collection("users").doc(ALICE_UID);

  await assertFails(aliceRef.get());
  await assertFails(aliceRef.update({ displayName: "Hacked", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceRef.collection("food_logs").get());
  await assertFails(aliceRef.collection("food_logs").doc("f_1").delete());
  await assertFails(aliceRef.collection("exercise_logs").get());
  await assertFails(aliceRef.collection("water_logs").doc("2026-11-21").get());
  await assertFails(aliceRef.collection("weight_logs").get());
  await assertFails(aliceRef.collection("favorite_foods").get());
});

test("Rejects @DocumentId 'id' field, invalid bounds, future timestamps, and invalid IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Extra 'id' field rejected
  await assertFails(
    userRef.collection("food_logs").doc("f_extra").set({
      id: "f_extra",
      ...validFoodPayload(ALICE_UID, ts),
    })
  );

  // Future timestamp rejected
  await assertFails(
    userRef.collection("food_logs").doc("f_future").set(
      validFoodPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Invalid calories (> 15000) rejected
  await assertFails(
    userRef.collection("food_logs").doc("f_big").set({
      ...validFoodPayload(ALICE_UID, ts),
      calories: 99999,
    })
  );

  // Invalid enum rejected
  await assertFails(
    userRef.collection("food_logs").doc("f_enum").set({
      ...validFoodPayload(ALICE_UID, ts),
      mealType: "breakfast",
    })
  );

  // Invalid dateKey rejected
  await assertFails(
    userRef.collection("water_logs").doc("bad_date").set({
      ...validWaterPayload(ALICE_UID, ts),
      dateKey: "21-11-2026",
    })
  );
});
