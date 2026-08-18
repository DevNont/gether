# 06 — Roadmap

ทำตามลำดับ อย่าข้าม แต่ละ task มี "เสร็จเมื่อไหร่" กำกับไว้ ให้ commit ทีละ task

---

## M1 — โครงและ domain (ทำก่อนแตะ UI)

- [ ] **M1.1** สร้างโปรเจกต์ Gradle หลายโมดูลตาม `docs/05-architecture.md` พร้อม version catalog
      *เสร็จเมื่อ:* `./gradlew assembleDebug` ผ่าน
- [ ] **M1.2** คัดลอก `code/domain/*.kt` เข้า `:core:domain` ตาม package ที่ระบุในไฟล์
- [ ] **M1.3** คัดลอก `code/test/MoneyLogicTest.kt` เข้า `:core:domain/src/test`
      *เสร็จเมื่อ:* `./gradlew :core:domain:test` เขียว ครบ 20 เคส
- [ ] **M1.4** เพิ่ม ktlint + detekt + CI workflow
- [ ] **M1.5** ตั้งค่า Firebase project, เปิด Auth/Firestore/Storage, deploy rules กับ indexes จาก `firebase/`
- [ ] **M1.6** เขียน repository interfaces ทั้งหมดใน `:core:domain` (ยังไม่ต้อง implement)

## M2 — Auth และทริป

- [ ] **M2.1** Google Sign-in ผ่าน Credential Manager + สร้าง `users/{uid}` ครั้งแรก
- [ ] **M2.2** `TripRepository` implementation + mapper + เทสต์บน emulator
- [ ] **M2.3** S02 TripList (2 กลุ่ม, empty state)
- [ ] **M2.4** S03 CreateTrip พร้อม batch write สร้าง days ทุกวัน
- [ ] **M2.5** S04 JoinTrip + deep link `triptogether://join/{code}` และ App Links
- [ ] **M2.6** S05 Overview + แชร์ลิงก์เชิญ
      *เสร็จเมื่อ:* สร้างทริปบนเครื่อง A แล้วเครื่อง B เข้าร่วมด้วยลิงก์ได้และเห็นข้อมูลเดียวกัน

## M3 — แผนการเดินทาง

- [ ] **M3.1** `PlanRepository` + realtime listener ต่อวัน
- [ ] **M3.2** S06 DayPlan timeline + แถบเลือกวัน
- [ ] **M3.3** S07 ActivityEditor + เปิด Google Maps intent
- [ ] **M3.4** แนบไฟล์ตั๋ว/ใบจองผ่าน Storage
      *เสร็จเมื่อ:* แก้แผนบนเครื่อง A แล้วเครื่อง B เห็นภายใน 2 วินาที

## M4 — ค่าใช้จ่าย (ส่วนที่สำคัญที่สุด)

- [ ] **M4.1** `ExpenseRepository` + mapper (เงินเป็น Long สตางค์)
- [ ] **M4.2** S08 ExpenseList จัดกลุ่มตามวัน + ตัวกรองหมวด
- [ ] **M4.3** S09 ExpenseEditor โหมดหารเท่า
- [ ] **M4.4** S09 โหมดใส่เอง + แถบ delta + ปุ่มเกลี่ยส่วนที่เหลือ + ปุ่มค่าบริการ
      *เสร็จเมื่อ:* กรอกยอดไม่ครบแล้วกดบันทึกไม่ได้ และแถบแดงบอกส่วนต่างถูกต้อง
- [ ] **M4.5** S09 โหมดตามสัดส่วน
- [ ] **M4.6** ถ่าย/แนบสลิปด้วย CameraX + บีบอัดก่อนอัปโหลด
- [ ] **M4.7** S10 ExpenseDetail + SlipViewer แบบ zoom
- [ ] **M4.8** Compose UI test ครอบ flow บันทึกบิลแบบหารไม่เท่ากัน

## M5 — สรุปยอด

- [ ] **M5.1** `GetBalancesUseCase` + `GetSuggestedTransfersUseCase` ต่อจาก domain ที่มีอยู่
- [ ] **M5.2** S11 Settlement: การ์ดสถิติ + ตารางยอดคงเหลือ + รายการโอน
- [ ] **M5.3** flow "โอนแล้ว" → `PENDING` → ผู้รับยืนยัน → `CONFIRMED`
- [ ] **M5.4** PromptPay QR generator + ทดสอบสแกนด้วยแอปธนาคารจริง 2 แอป
      *เสร็จเมื่อ:* ทริปทดสอบ 5 คน 15 บิล ปิดยอดได้ครบ ทุกคน balance = 0

**จบ M5 = MVP พร้อมปล่อยให้กลุ่มทดสอบใช้ทริปจริง**

---

## M6 — เสริมกลุ่ม

- [ ] **M6.1** Cloud Functions + FCM (บิลใหม่, แผนเปลี่ยน, มีคนโอน)
- [ ] **M6.2** S12 Checklist ส่วนกลาง/ส่วนตัว
- [ ] **M6.3** S13 โหวตร้าน/สถานที่
- [ ] **M6.4** Guest member (สมาชิกที่ไม่มีบัญชี)
- [ ] **M6.5** S14 Settings ครบ + ตั้งค่าการแจ้งเตือน

## M7 — ขัดเงา

- [ ] **M7.1** Dark mode + dynamic color
- [ ] **M7.2** แถบสถานะออฟไลน์ + จัดการ error ให้ครบทุกหน้า
- [ ] **M7.3** Empty state และ shimmer ทุกลิสต์
- [ ] **M7.4** แปลภาษาอังกฤษ (`values-en/`)
- [ ] **M7.5** Baseline profile + วัด startup time
- [ ] **M7.6** Crashlytics + Analytics event หลัก

## M8 — ภายหลัง

- [ ] แยกบิลรายจาน (item-level split)
- [ ] Multi-currency พร้อมเรตต่อทริป
- [ ] Export สรุปทริปเป็น PDF
- [ ] ย้าย `:core:domain` เป็น KMP module
- [ ] iOS (SwiftUI + Firebase iOS SDK)

---

## กฎการ commit

หนึ่ง task = หนึ่ง commit ข้อความสั้น ๆ ภาษาอังกฤษ นำหน้าด้วยรหัส task

```
M4.4 add exact-split validation with running delta bar
M4.4 fix rounding when distributing remainder to unfilled members
```

ห้าม merge PR ที่ `./gradlew test` ไม่เขียว โดยเฉพาะเทสต์ใน `:core:domain`
