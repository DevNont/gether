/**
 * TripTogether Cloud Functions — docs/05 table.
 * Deploy requires the Blaze plan; code kept ready until then.
 */
import * as admin from "firebase-admin";
import { logger } from "firebase-functions";
import {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentWritten,
} from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";

admin.initializeApp();
const db = admin.firestore();

const REGION = "asia-southeast1";
const ACTIVITY_DEBOUNCE_MS = 5 * 60 * 1000;

interface Share {
  memberId: string;
  amount: number;
}

/** Push to every trip member except the actor. Tokens come from users/{uid}.fcmTokens. */
async function notifyTripMembers(
  tripId: string,
  excludeMemberId: string | null,
  payload: { title: string; body: string; screen: string },
): Promise<void> {
  const members = await db.collection(`trips/${tripId}/members`).get();
  const userIds = members.docs
    .filter((doc) => doc.id !== excludeMemberId)
    .map((doc) => doc.get("userId") as string | null)
    .filter((id): id is string => typeof id === "string" && id.length > 0);
  if (userIds.length === 0) return;

  const tokens: string[] = [];
  for (const userId of userIds) {
    const user = await db.doc(`users/${userId}`).get();
    const userTokens = (user.get("fcmTokens") as string[] | undefined) ?? [];
    tokens.push(...userTokens);
  }
  if (tokens.length === 0) return;

  // tripId + screen let the app deep-open the right page on tap (docs/05).
  await admin.messaging().sendEachForMulticast({
    tokens,
    notification: { title: payload.title, body: payload.body },
    data: { tripId, screen: payload.screen },
  });
}

/** Validates the money invariant and notifies members about new/changed bills. */
export const onExpenseWrite = onDocumentWritten(
  { document: "trips/{tripId}/expenses/{expenseId}", region: REGION },
  async (event) => {
    const after = event.data?.after;
    if (!after?.exists) return;

    const total = after.get("totalAmount") as number;
    const shares = (after.get("shares") as Share[] | undefined) ?? [];
    const sum = shares.reduce((acc, share) => acc + share.amount, 0);
    if (sum !== total) {
      // Client and rules should have blocked this; log loudly, never auto-correct.
      logger.error("Expense shares do not sum to total", {
        tripId: event.params.tripId,
        expenseId: event.params.expenseId,
        total,
        sum,
      });
    }

    const isCreate = !event.data?.before?.exists;
    if (isCreate) {
      await notifyTripMembers(event.params.tripId, after.get("createdBy") as string, {
        title: "บิลใหม่",
        body: `${after.get("title")}`,
        screen: "expenseDetail",
      });
    }
  },
);

/** Tells the receiver someone marked a transfer to them; they confirm in-app. */
export const onSettlementCreate = onDocumentCreated(
  { document: "trips/{tripId}/settlements/{settlementId}", region: REGION },
  async (event) => {
    const settlement = event.data;
    if (!settlement) return;

    const toMemberId = settlement.get("toMemberId") as string;
    const member = await db
      .doc(`trips/${event.params.tripId}/members/${toMemberId}`)
      .get();
    const userId = member.get("userId") as string | null;
    if (!userId) return;

    const user = await db.doc(`users/${userId}`).get();
    const tokens = (user.get("fcmTokens") as string[] | undefined) ?? [];
    if (tokens.length === 0) return;

    await admin.messaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "มีคนโอนเงินให้คุณ",
        body: "แตะเพื่อยืนยันการรับเงิน",
      },
      data: { tripId: event.params.tripId, screen: "settlement" },
    });
  },
);

/** Plan-change pushes, debounced to at most one per trip per 5 minutes (docs/05). */
export const onActivityWrite = onDocumentWritten(
  { document: "trips/{tripId}/days/{dayId}/activities/{activityId}", region: REGION },
  async (event) => {
    const tripId = event.params.tripId;
    const markerRef = db.doc(`trips/${tripId}/meta/planNotifyDebounce`);
    const now = Date.now();

    const shouldSend = await db.runTransaction(async (transaction) => {
      const marker = await transaction.get(markerRef);
      const last = (marker.get("lastSentAt") as number | undefined) ?? 0;
      if (now - last < ACTIVITY_DEBOUNCE_MS) return false;
      transaction.set(markerRef, { lastSentAt: now });
      return true;
    });
    if (!shouldSend) return;

    await notifyTripMembers(tripId, null, {
      title: "แผนทริปมีการเปลี่ยนแปลง",
      body: "แตะเพื่อดูแผนล่าสุด",
      screen: "dayPlan",
    });
  },
);

/** Daily sweep: deactivate invite codes past their expiry. */
export const cleanupExpiredInvites = onSchedule(
  { schedule: "every day 03:00", region: REGION, timeZone: "Asia/Bangkok" },
  async () => {
    const expired = await db
      .collection("inviteCodes")
      .where("active", "==", true)
      .where("expiresAt", "<", admin.firestore.Timestamp.now())
      .get();
    const batch = db.batch();
    expired.docs.forEach((doc) => batch.update(doc.ref, { active: false }));
    await batch.commit();
    logger.info(`Deactivated ${expired.size} expired invite codes`);
  },
);

/** Removes subcollections, the invite code, and Storage files when a trip dies. */
export const onTripDelete = onDocumentDeleted(
  { document: "trips/{tripId}", region: REGION },
  async (event) => {
    const tripId = event.params.tripId;

    const inviteCode = event.data?.get("inviteCode") as string | undefined;
    if (inviteCode) {
      await db.doc(`inviteCodes/${inviteCode}`).delete().catch(() => undefined);
    }

    await db.recursiveDelete(db.doc(`trips/${tripId}`));

    await admin
      .storage()
      .bucket()
      .deleteFiles({ prefix: `trips/${tripId}/` })
      .catch((error) => logger.warn("Storage cleanup failed", { tripId, error }));
  },
);
