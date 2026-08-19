# TODO: Push Notifications (FCM)

เก็บงานที่ต้องทำเพื่อเปิดใช้การแจ้งเตือน push — ตอนนี้เมนู Settings > Notifications เป็น placeholder (`enabled=false`) และ Cloud Functions เขียนไว้แล้วแต่ยังไม่ deploy

## Blocker หลัก
- [ ] **Blaze plan** — Cloud Functions + FCM deploy ไม่ได้ถ้าไม่อัปเกรด (docs/06 M6.1)

## Cloud Functions (เขียนเสร็จแล้ว — `firebase/functions/src/index.ts`, tsc ผ่าน, รอ deploy)
มี 3 trigger:
- [ ] `onExpenseWrite` (`expenses/{id}`) → "บิลใหม่ — [ชื่อบิล]"
- [ ] `onSettlementCreate` (`settlements/{id}`) → "มีคนโอนเงินให้คุณ — แตะเพื่อยืนยัน" (ส่งเฉพาะผู้รับ)
- [ ] `onActivityWrite` (`activities/{id}`, debounce 5 นาที) → "แผนทริปมีการเปลี่ยนแปลง"
- ส่งให้สมาชิกคนอื่น (ไม่ส่งกลับคนทำเอง) ผ่าน `users/{uid}.fcmTokens`; payload มี `tripId` + `screen`
- [ ] `firebase deploy --only functions` (หลัง Blaze)

## Client FCM wiring (แอป — ยังไม่ทำ)
- [ ] เพิ่ม dep `firebase-messaging` (bundle firebase มีแล้วใน catalog — เช็ค)
- [ ] ขอ permission `POST_NOTIFICATIONS` (Android 13+) ตอน sign-in หรือครั้งแรกที่เข้าทริป
- [ ] รับ FCM token → เก็บ/อัปเดตใน `users/{uid}.fcmTokens` (array, รองรับหลายเครื่อง) + ลบ token เก่าเมื่อ sign-out
- [ ] `FirebaseMessagingService`: handle message ตอน foreground + สร้าง notification channel
- [ ] แตะ notification → deep link ไป `tripId` + `screen` (ต่อกับ NavHost — มี pattern deep link `triptogether://` อยู่แล้วใน MainActivity)

## Settings > Notifications screen (UI — docs/03:127, roadmap M6.5)
- [ ] เปิดเมนูแถว Notifications ใน `SettingsScreen` (ตอนนี้ `enabled=false`)
- [ ] หน้าใหม่ `SettingsNotificationsScreen` (mirror `SettingsLanguageScreen`/`SettingsThemeScreen`)
- [ ] toggle แยกตามประเภท: **บิลใหม่ / แผนเปลี่ยน / เรื่องเงิน** — เก็บ pref (SharedPreferences หรือ `users/{uid}`)
- [ ] Cloud Functions อ่าน pref ผู้รับ ก่อนส่ง (ถ้าเลือกไม่ใช่ประเภทนั้น → ข้าม) — ถ้าเก็บ pref ใน Firestore
- [ ] route ใน `AuthNav` + wiring `AppNavHost` (แบบเดียวกับ theme/language)

## ลำดับแนะนำ
1. เปิด Blaze → deploy functions (ยิงได้ แต่ client ยังไม่รับ)
2. Client: permission + token + messaging service + tap→deep link (แจ้งเตือนเด้งได้)
3. Settings toggle UI + ให้ functions เคารพ pref
4. เปิดเมนู Notifications ใน Settings

## strings ที่ต้องเพิ่ม (feature/auth th/en)
`settings_notifications_bills`, `settings_notifications_plan`, `settings_notifications_money`, ฯลฯ
