# TODO: เตรียมขึ้น Play Store

รวมทุกอย่างที่ต้องทำก่อนปล่อยแอปบน Google Play — ยังไม่ได้ลงมือ เก็บไว้เป็น checklist
สถานะปัจจุบัน: แอปปล่อยเป็น **debug-signed APK บน GitHub Releases** (beta ให้เพื่อน test) เท่านั้น

> ⚠️ LINE login (OIDC browser flow) **ยังต้องลงทะเบียน SHA-1** — Firebase เช็ค cert hash (`INVALID_CERT_HASH`) ตอนเริ่ม flow. Play เซ็นใหม่ = SHA-1 ใหม่ ต้องเพิ่มใน Firebase ทั้ง upload key และ app signing key (ดู `docs/09`)

---

## 1. Signing — สร้าง release keystore จริง (เลิกใช้ debug key)

- [ ] สร้าง keystore (ทำครั้งเดียว เก็บให้ดี **หายแล้วอัปเดตแอปบน Play ไม่ได้**):
  ```bash
  keytool -genkeypair -v -keystore triptogether-release.jks \
    -alias triptogether -keyalg RSA -keysize 2048 -validity 10000
  ```
- [ ] เก็บ keystore ไว้นอก repo + backup (เช่น password manager / drive ส่วนตัว)
- [ ] สร้าง `keystore.properties` ที่ root (ใส่ใน `.gitignore` แล้ว — `*.jks` ก็ ignore แล้ว):
  ```properties
  storeFile=/absolute/path/triptogether-release.jks
  storePassword=***
  keyAlias=triptogether
  keyPassword=***
  ```
- [ ] `app/build.gradle.kts` — เพิ่ม release signingConfig อ่านจาก `keystore.properties`
      (ตอน CI/ไม่มีไฟล์ → fallback ข้าม หรือใช้ env var แบบ workflow release ปัจจุบัน)

## 2. Build เป็น App Bundle + เปิด R8

- [ ] `./gradlew bundleRelease` → ได้ `.aab` (Play รับ `.aab` ไม่ใช่ `.apk`)
- [ ] `buildTypes.release`: `isMinifyEnabled = true` + `isShrinkResources = true`
- [ ] เพิ่ม proguard/R8 keep rules ให้ครบ (จุดที่ใช้ reflection):
  - Firebase Firestore DTO (`@Keep` หรือ keep คลาส `com.triptogether.core.data.dto.**`)
  - kotlinx.serialization (`@Serializable` nav routes)
  - Hilt / Dagger generated
  - Compose (ปกติมี rule ให้แล้ว)
  - ZXing
- [ ] ทดสอบ `bundleRelease` แล้ว build APK จาก bundle (`bundletool`) ลงเครื่องจริง เช็คว่า minify ไม่ทำ Firestore mapping พัง

## 3. Firebase — เพิ่ม SHA-1 (สำคัญสุด)

- [ ] Play Console > **App signing** → copy SHA-1 **2 อัน**:
  - **Upload key certificate** SHA-1
  - **App signing key certificate** SHA-1 (Google เซ็นให้ตอนส่งถึงเครื่อง user)
- [ ] Firebase Console > Project settings > Android app (`com.triptogether`) → **Add fingerprint** ทั้ง 2 อัน
- [ ] โหลด `google-services.json` ใหม่มาวางที่ `app/` (gitignored)
- [ ] อัปเดต secret `GOOGLE_SERVICES_JSON` ใน GitHub ด้วย (ถ้า CI ยัง build)
- [ ] ทดสอบ Google sign-in บน build ที่เซ็นด้วย upload key จริง

## 4. Play Console — สร้างแอป + store listing

- [ ] สร้างแอปใน Play Console, package `com.triptogether`
- [ ] เปิด **Play App Signing** (แนะนำ — Google ถือ signing key ให้)
- [ ] Store listing: ชื่อ, คำอธิบาย (ไทย+อังกฤษ), ไอคอน 512px, feature graphic, screenshots
- [ ] **Data safety form** — ต้องประกาศว่าเก็บ email + ข้อมูลทริป, เก็บบน Firebase, เข้ารหัส transit
- [ ] **Privacy policy** URL (บังคับ เพราะมี account/email) — ต้องมีหน้าเว็บ
- [ ] Content rating questionnaire
- [ ] Target audience / ads = ไม่มีโฆษณา
- [ ] เลือก countries + rollout (แนะนำ closed/internal testing track ก่อน)

## 5. targetSdk / policy

- [ ] Play บังคับ targetSdk ล่าสุด (ปัจจุบัน 35 — เช็ค deadline ปีนั้นๆ)
- [ ] `versionCode` +1 ทุก release (ตอนนี้ 4 = v0.1.3); Play ไม่รับ versionCode ซ้ำ

## 6. External blockers (ยังค้าง — ไม่บังคับสำหรับ MVP)

- [ ] **Blaze plan** — จำเป็นถ้าจะเปิด: รูปสลิป/โปรไฟล์ (Storage), FCM push (Cloud Functions)
- [ ] Auth + Firestore ทำงานบน **Spark (free) ได้** — เริ่มขายจริงยังไม่ต้อง Blaze จนกว่าจะเกิน quota

## 7. (ทางเลือก) CI สำหรับ Play

- [ ] workflow แยก build `.aab` + เซ็น upload key จาก secret
- [ ] (ขั้นสูง) `r0adkll/upload-google-play` auto-upload ไป internal track (ต้องมี service account JSON จาก Play Console)

---

## ลำดับแนะนำ

1. สร้าง release keystore + signingConfig (ข้อ 1)
2. เปิด minify + proguard rules, ทดสอบ bundleRelease บนเครื่องจริง (ข้อ 2)
3. สร้างแอปใน Play Console + เปิด App Signing → ได้ SHA-1 (ข้อ 4 บางส่วน)
4. เพิ่ม SHA-1 ทั้งสองใน Firebase + โหลด google-services.json ใหม่ (ข้อ 3)
5. ทดสอบ sign-in บน internal testing track
6. ทำ store listing + data safety + privacy policy (ข้อ 4 ที่เหลือ)
7. Rollout
