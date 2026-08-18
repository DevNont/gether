# TripTogether — ชุดไฟล์ส่งมอบให้ Claude Code

แอป Android วางแผนทริปกลุ่ม + หารค่าอาหาร (iOS ทำภายหลัง)

## เริ่มยังไง

1. แตกไฟล์ทั้งหมดไว้ในโฟลเดอร์โปรเจกต์เปล่า
2. เปิด terminal ที่โฟลเดอร์นั้นแล้วรัน `claude`
3. พิมพ์คำสั่งแรก:

```
อ่าน CLAUDE.md และ docs/ ทั้งหมดก่อน แล้วเริ่มทำ M1.1 ตาม docs/06-roadmap.md
```

Claude Code จะอ่าน `CLAUDE.md` อัตโนมัติทุกครั้งที่เริ่ม session ใหม่ จึงไม่ต้องอธิบายบริบทซ้ำ

## มีอะไรอยู่ในนี้

```
CLAUDE.md                    กฎและบริบทที่ Claude Code อ่านอัตโนมัติ
README.md                    ไฟล์นี้

docs/
  01-product-spec.md         ฟีเจอร์ + user story 31 ข้อ + non-goals
  02-data-model.md           Firestore ทุก collection ทุก field + indexes
  03-screens.md              navigation graph + รายละเอียด 14 หน้าจอ
  04-money-logic.md          ⭐ สเปกการหารเงิน + 20 test case ที่ต้องผ่าน
  05-architecture.md         โมดูล Gradle, package, repository interface, CI
  06-roadmap.md              checklist งาน M1–M8 พร้อมเกณฑ์ "เสร็จเมื่อไหร่"

mockups/
  all-screens.html           mockup ครบ 14 หน้า เปิดในเบราว์เซอร์ได้เลย

code/
  domain/Money.kt            value class เงินหน่วยสตางค์
  domain/Models.kt           domain model ทั้งหมด
  domain/ExpenseSplitter.kt  ⭐ algorithm การหาร 3 โหมด
  domain/DebtSimplifier.kt   ⭐ คำนวณ balance + ลดจำนวนครั้งการโอน
  test/MoneyLogicTest.kt     เทสต์ T1–T20 ครอบทุกเคสใน 04-money-logic.md

firebase/
  firestore.rules            security rules
  storage.rules              rules ของไฟล์สลิป/ไฟล์แนบ
  firestore.indexes.json     composite indexes 4 ตัว
  firebase.json              config + emulator ports

gradle/
  libs.versions.toml         version catalog ปักหมุดเวอร์ชันไว้แล้ว
```

## สิ่งที่ควรรู้ก่อนเริ่ม

**โค้ดใน `code/` เขียนเสร็จแล้วและตรวจสอบ algorithm มาแล้ว** ทุกเคสใน `docs/04-money-logic.md` ถูกรันจริงกับ reference implementation แล้วผ่านหมด — ให้คัดลอกไปใช้ อย่าให้ Claude Code เขียนใหม่เพราะเป็นส่วนที่ผิดแล้วเสียหายที่สุด

**ทำ M1 ให้จบก่อนแตะ UI** โครงโมดูล + domain + เทสต์เขียว แล้วค่อยทำหน้าจอ เพราะทุกหน้าที่เกี่ยวกับเงินเรียกใช้ domain ที่เดียวกัน

**ต้องเตรียมเองก่อนรัน M1.5:**
- สร้าง Firebase project แล้วดาวน์โหลด `google-services.json` มาไว้ที่ `app/`
- เปิด Authentication (Google provider), Firestore, Storage
- ใส่ SHA-1 ของ debug keystore ใน Firebase Console ไม่งั้น Google Sign-in จะล้มเหลว
- `google-services.json` ต้องอยู่ใน `.gitignore` ตั้งแต่ commit แรก

**สิ่งที่ยังไม่ได้ตัดสินใจและควรถามก่อนทำ:**
- ชื่อแอปจริงและ package name (ตอนนี้ใช้ `com.triptogether` เป็น placeholder)
- จะใช้ Places API สำหรับค้นหาสถานที่ไหม (มีค่าใช้จ่าย) หรือให้พิมพ์ชื่อสถานที่เอง
- จะเก็บ analytics อะไรบ้าง

## ลำดับความสำคัญถ้าเวลาไม่พอ

ถ้าต้องตัดฟีเจอร์ ให้ตัดจากท้ายรายการนี้: **หารเงิน → สรุปยอด → แผนรายวัน → join ทริป → สลิป → checklist → โหวต → QR พร้อมเพย์**

สองอย่างแรกคือเหตุผลที่คนจะโหลดแอป ที่เหลือคือของแถม
