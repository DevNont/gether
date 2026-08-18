# 02 — Data model (Cloud Firestore)

## หลักการออกแบบ

1. **ข้อมูลของทริปอยู่ใต้ `trips/{tripId}` ทั้งหมด** — security rules เขียนง่าย (เช็คครั้งเดียวว่าเป็นสมาชิกไหม) และดึงทริปหนึ่งจบในไม่กี่ query
2. **`memberId` ไม่ใช่ `userId`** — เพราะต้องรองรับ guest member ที่ไม่มีบัญชี ทุกอย่างที่เกี่ยวกับเงินอ้าง `memberId` เสมอ
3. **เงินเก็บเป็น integer หน่วยสตางค์** (`Long`) ทุกที่ — `185000` = 1,850.00 บาท ไม่มี field ไหนเป็น float
4. **denormalize ชื่อและรูปสมาชิก** ลงใน member doc เพื่อไม่ต้อง join กับ `users` ตอนแสดงรายการบิล

---

## Collection tree

```
users/{userId}
trips/{tripId}
  members/{memberId}
  days/{dayId}
    activities/{activityId}
  expenses/{expenseId}
  settlements/{settlementId}
  checklist/{itemId}
  polls/{pollId}
inviteCodes/{code}
```

---

## `users/{userId}`

`userId` = Firebase Auth UID

| field | type | หมายเหตุ |
|---|---|---|
| `displayName` | string | |
| `photoUrl` | string? | |
| `promptpayId` | string? | เบอร์มือถือหรือเลขบัตร ใช้ gen QR |
| `fcmTokens` | array\<string\> | รองรับหลายเครื่อง |
| `createdAt` | timestamp | |

เจ้าตัวอ่าน/เขียนได้เท่านั้น สมาชิกทริปเดียวกันอ่าน `displayName`/`photoUrl`/`promptpayId` ได้ผ่าน member doc ที่ denormalize ไว้แล้ว

---

## `trips/{tripId}`

| field | type | หมายเหตุ |
|---|---|---|
| `name` | string | |
| `coverUrl` | string? | |
| `startDate` | string | `yyyy-MM-dd` เก็บเป็น string เพื่อเลี่ยงปัญหา timezone |
| `endDate` | string | `yyyy-MM-dd` |
| `ownerId` | string | userId ของเจ้าของ |
| `memberIds` | array\<string\> | **userId** ของสมาชิกที่มีบัญชี ใช้ใน security rules และ `array-contains` query |
| `currency` | string | `THB` (เตรียมไว้เฉย ๆ v1 ล็อกที่ THB) |
| `inviteCode` | string | 6 ตัวอักษร A–Z 0–9 |
| `archived` | boolean | |
| `createdAt` / `updatedAt` | timestamp | |

**Query หลัก:** `trips.where('memberIds', 'array-contains', uid).orderBy('startDate', 'desc')`

---

## `trips/{tripId}/members/{memberId}`

`memberId` = auto-id (ไม่ใช่ userId เพราะ guest ไม่มี userId)

| field | type | หมายเหตุ |
|---|---|---|
| `userId` | string? | `null` = guest member |
| `displayName` | string | denormalized |
| `photoUrl` | string? | denormalized |
| `promptpayId` | string? | denormalized ใช้ gen QR |
| `role` | string | `owner` \| `member` |
| `joinedAt` | timestamp | |

---

## `trips/{tripId}/days/{dayId}`

`dayId` = `yyyy-MM-dd` (ใช้วันที่เป็น doc id เลย — sort ได้ในตัว ไม่มีวันซ้ำ)

| field | type |
|---|---|
| `date` | string `yyyy-MM-dd` |
| `note` | string? |

> วันถูกสร้างอัตโนมัติจาก `startDate`–`endDate` ตอนสร้างทริป และปรับเมื่อแก้ช่วงวันที่ (ดู `docs/03-screens.md` หัวข้อ edit trip)

## `trips/{tripId}/days/{dayId}/activities/{activityId}`

| field | type | หมายเหตุ |
|---|---|---|
| `title` | string | |
| `startTime` | string? | `HH:mm` |
| `endTime` | string? | `HH:mm` |
| `placeName` | string? | |
| `lat` / `lng` | number? | ใช้เปิด Maps |
| `note` | string? | |
| `attachments` | array\<{name, url, mimeType}\> | ไฟล์ใน Storage |
| `sortOrder` | number | ใช้เรียงเมื่อไม่ระบุเวลา |
| `createdBy` | string | memberId |

---

## `trips/{tripId}/expenses/{expenseId}` ⭐

| field | type | หมายเหตุ |
|---|---|---|
| `title` | string | เช่น "มื้อเย็น — ร้านหมูกระทะ" |
| `category` | string | `food` \| `stay` \| `transport` \| `activity` \| `other` |
| `totalAmount` | number (Long) | **สตางค์** |
| `paidByMemberId` | string | คนที่ควักจ่ายจริง |
| `date` | string | `yyyy-MM-dd` ใช้จัดกลุ่มในรายการ |
| `splitMode` | string | `EQUAL` \| `EXACT` \| `SHARES` |
| `shares` | array\<Share\> | **ฝังไว้ในเอกสารเดียวกัน** ดูด้านล่าง |
| `slipUrls` | array\<string\> | รูปสลิปใน Storage |
| `note` | string? | |
| `createdBy` | string | memberId |
| `createdAt` / `updatedAt` | timestamp | |

**Share (embedded object):**

| field | type | หมายเหตุ |
|---|---|---|
| `memberId` | string | |
| `amount` | number (Long) | สตางค์ที่คนนี้ต้องรับผิดชอบ |
| `weight` | number? | ใช้เฉพาะ `SHARES` mode (เช่น 1, 1, 2) |

> **ทำไม embed ไม่แยก subcollection:** ทริปหนึ่งมีสมาชิกไม่เกิน ~15 คน ดังนั้น shares ต่อบิลไม่เกิน 15 รายการ — ยังห่างจากลิมิต 1 MB ต่อ document มาก และการ embed ทำให้อ่านบิลจบใน 1 read กับแก้ทั้งบิลเป็น atomic write เดียว ไม่ต้องใช้ transaction

**Invariant ที่ต้องเป็นจริงเสมอ:** `shares.sumOf { it.amount } == totalAmount`

**Query หลัก:** `expenses.orderBy('date', 'desc').orderBy('createdAt', 'desc')`

---

## `trips/{tripId}/settlements/{settlementId}`

บันทึกการโอนจริงที่เกิดขึ้น (ไม่ใช่รายการที่ระบบแนะนำ — อันนั้นคำนวณสด ๆ ทุกครั้ง)

| field | type | หมายเหตุ |
|---|---|---|
| `fromMemberId` | string | |
| `toMemberId` | string | |
| `amount` | number (Long) | สตางค์ |
| `status` | string | `PENDING` \| `CONFIRMED` |
| `markedBy` | string | memberId คนที่กด "โอนแล้ว" |
| `confirmedBy` | string? | memberId คนรับที่ยืนยัน |
| `slipUrl` | string? | |
| `createdAt` / `confirmedAt` | timestamp | |

> เฉพาะ `CONFIRMED` เท่านั้นที่ถูกนำไปหักในการคำนวณยอดคงเหลือ

---

## `trips/{tripId}/checklist/{itemId}`

| field | type | หมายเหตุ |
|---|---|---|
| `title` | string | |
| `scope` | string | `PERSONAL` \| `SHARED` |
| `assignedMemberId` | string? | ใช้กับ `SHARED` |
| `checkedBy` | array\<string\> | memberId — `PERSONAL` ให้แต่ละคนติ๊กของตัวเอง |
| `sortOrder` | number | |

## `trips/{tripId}/polls/{pollId}`

| field | type | หมายเหตุ |
|---|---|---|
| `question` | string | |
| `options` | array\<{id, label, voterMemberIds}\> | |
| `multiChoice` | boolean | |
| `closesAt` | timestamp? | |
| `closed` | boolean | |

---

## `inviteCodes/{code}`

top-level collection เพื่อให้คนที่ยังไม่ใช่สมาชิกอ่านได้ (สมาชิกทริปเท่านั้นที่อ่าน `trips/{tripId}` ได้)

| field | type |
|---|---|
| `tripId` | string |
| `tripName` | string |
| `active` | boolean |
| `expiresAt` | timestamp? |

`code` = doc id, 6 ตัวอักษร ตัดตัวที่สับสน (`O`, `0`, `I`, `1`) ออก

---

## Composite indexes ที่ต้องสร้าง

ดูไฟล์ `firebase/firestore.indexes.json` — สร้างไว้ให้แล้ว 3 ตัว:
- `trips`: `memberIds` (array-contains) + `startDate` desc
- `expenses`: `date` desc + `createdAt` desc
- `expenses`: `category` asc + `date` desc

---

## Storage paths

```
trips/{tripId}/slips/{expenseId}/{uuid}.jpg      รูปสลิปบิล
trips/{tripId}/attachments/{activityId}/{file}   ตั๋ว/ใบจอง
trips/{tripId}/cover.jpg                         รูปปกทริป
users/{userId}/avatar.jpg                        รูปโปรไฟล์
```

บีบอัดรูปสลิปที่ฝั่ง client ก่อนอัปโหลด: ด้านยาวสุดไม่เกิน 1600px, JPEG quality 80, เพดาน 2 MB
