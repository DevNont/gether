# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# TripTogether — คำสั่งสำหรับ Claude Code

แอป Android สำหรับวางแผนท่องเที่ยวแบบกลุ่ม + หารค่าอาหาร (iOS ทำภายหลัง)

## สถานะปัจจุบัน

โครงโปรเจกต์ครบทุกโมดูลแล้ว ทำถึง M7 ตาม `docs/06-roadmap.md` (ดู git log — commit ตั้งชื่อตามรหัส task)
Firebase project = `triptogether-703c3` (Firestore asia-southeast1, rules/indexes deploy แล้ว; Auth = LINE `oidc.line` + Anonymous — ต้องตั้งค่า console ตาม `docs/09-line-login-setup.md`)

**สิ่งที่ยังทำไม่ได้เพราะติด blocker ภายนอก:**
- Storage bucket ยังไม่มี (ต้อง upgrade Blaze) → M3.4, M4.6, รูปปก, รูปโปรไฟล์, และ `storage.rules` ยังไม่ deploy
- Cloud Functions (`firebase/functions/`) เขียนเสร็จ tsc ผ่าน แต่ deploy ต้อง Blaze → FCM ฝั่ง client ยังไม่ต่อ
- เทสต์บนเครื่องจริง/emulator ยังไม่เคยรัน: sign-in E2E, เกณฑ์ multi-device (M2.6/M3), Compose UI test (M4.8), สแกน QR ด้วยแอปธนาคาร 2 แอป (บังคับก่อน release), M7.5 baseline profile
- `google-services.json` อยู่ที่ `app/` (gitignored) — CI ใช้ placeholder ที่สร้างใน workflow

## อ่านก่อนเริ่มงานเสมอ

| ไฟล์ | เนื้อหา |
|---|---|
| `docs/01-product-spec.md` | ฟีเจอร์ + user story ทั้งหมด |
| `docs/02-data-model.md` | โครงสร้าง Firestore ทุก collection |
| `docs/03-screens.md` | รายละเอียดทุกหน้าจอ + state |
| `docs/04-money-logic.md` | **สเปกการหารเงินและสรุปยอด — ห้ามเดาเอง** |
| `docs/05-architecture.md` | โมดูล, package, การตั้งชื่อ |
| `docs/06-roadmap.md` | ลำดับงาน แบ่งเป็นเฟส |
| `mockups/all-screens.html` | mockup ครบทุกหน้า เปิดในเบราว์เซอร์ |
| `code/` | โค้ด domain layer ที่เขียนไว้แล้ว — คัดลอกไปใช้ อย่าเขียนใหม่ |

## Tech stack (ตายตัว — อย่าเปลี่ยนโดยไม่ถาม)

- Kotlin 2.x, Jetpack Compose + Material 3, minSdk 26, targetSdk 35
- MVVM: `ui` → `domain` → `data` (dependency ชี้ทางเดียวเข้าใน)
- Hilt (DI), Coroutines + Flow, Navigation Compose แบบ type-safe
- Firebase: Auth (LINE ผ่าน OIDC `oidc.line` + Anonymous — ดู `docs/09-line-login-setup.md`), Firestore, Storage, FCM
- Coil (รูป), CameraX (ถ่ายสลิป)
- Test: JUnit5, Turbine, MockK, Compose UI Test
- Gradle version catalog ที่ `gradle/libs.versions.toml` (ปักหมุดเวอร์ชันไว้แล้ว)

## โครงโมดูล (รายละเอียดเต็มใน `docs/05-architecture.md`)

```
:app             NavHost + DI wiring — ที่เดียวที่ผูก core:data เข้ากับ interface ใน core:domain
:core:domain     models, use cases, repository interfaces, money logic (ห้ามพึ่ง Android/Firebase)
:core:data       Firestore/Storage/Auth implementations + DTO + mapper
:core:ui         theme, Composable ใช้ร่วม
:core:testing    fake repositories, data builders
:feature:*       auth, trip, plan, expense, settlement, extras
```

กฎ dependency: `feature:*` พึ่งได้แค่ `core:domain` + `core:ui` เท่านั้น — ห้าม feature พึ่ง feature อื่น ห้ามพึ่ง `core:data` ตรง
Repository ทุก method คืน `Result<T>` ไม่ throw ข้าม layer / one-shot event ใช้ `Channel(BUFFERED).receiveAsFlow()` ไม่ใส่ใน state

## กฎที่ห้ามละเมิด

1. **เงินเป็น `Long` หน่วยสตางค์เสมอ** — ห้ามใช้ `Double`/`Float` กับจำนวนเงินทุกกรณี รวมถึงตัวแปรชั่วคราว ใช้ value class `Money` ใน `code/domain/Money.kt`
2. **ห้ามให้ผลรวมของ share ไม่เท่ากับยอดบิล** — validate ทั้งฝั่ง client และ Firestore rules
3. **`domain` layer ห้าม import อะไรที่ขึ้นกับ Android หรือ Firebase** — เตรียมย้ายไป KMP ตอนทำ iOS
4. **ห้ามอ่าน/เขียน Firestore ตรงจาก ViewModel** — ผ่าน Repository interface ใน `domain` เท่านั้น
5. **ห้าม hardcode ข้อความ UI** — ใส่ `strings.xml` ทั้งหมด ภาษาหลักคือไทย มี `values-en/` ด้วย
6. **ทุก use case ที่เกี่ยวกับเงินต้องมี unit test** — ดูเทสต์ตัวอย่างใน `code/test/`
7. ห้าม commit `google-services.json` — ใส่ `.gitignore` ตั้งแต่ commit แรก

## สไตล์โค้ด

- ktlint + detekt รันใน CI ก่อน merge
- Composable: `PascalCase`, ไฟล์ตั้งชื่อตาม screen เช่น `ExpenseEditorScreen.kt`
- State ใช้ `data class XxxUiState` ตัวเดียวต่อหน้าจอ + `sealed interface XxxEvent` สำหรับ one-shot event
- ทุก Composable หลักต้องมี `@Preview` อย่างน้อย 1 ตัว
- คอมเมนต์เขียนภาษาอังกฤษ ข้อความ UI เป็นภาษาไทย

## คำสั่งที่ใช้บ่อย

```bash
./gradlew assembleDebug          # build
./gradlew test                   # unit test ทั้งหมด
./gradlew :core:domain:test      # เทสต์ logic การเงินอย่างเดียว
./gradlew ktlintFormat detekt    # จัด format + lint
cd firebase && firebase deploy --only firestore   # deploy rules + indexes (firebase.json อยู่ใน firebase/)
cd firebase && firebase emulators:start           # รัน emulator ตอน dev
```

## ลำดับการทำงานที่แนะนำ

ทำตาม `docs/06-roadmap.md` ทีละ task อย่าข้าม เริ่มจาก M1 (โครงโปรเจกต์ + domain + เทสต์) ก่อนแตะ UI เพราะ logic การเงินคือส่วนที่ผิดแล้วเจ็บที่สุด
