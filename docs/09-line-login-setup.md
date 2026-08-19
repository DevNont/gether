# 09 — LINE Login setup (งานฝั่ง console ต้องทำก่อนปุ่ม LINE ใช้ได้)

แอปใช้ Firebase OIDC provider `oidc.line` (browser flow) — ไม่ใช้ LINE SDK, ไม่ต้องมี backend
โค้ดฝั่งแอปเสร็จแล้ว ปุ่ม "เข้าสู่ระบบด้วย LINE" จะ error จนกว่าจะตั้งค่าครบ 3 ขั้นนี้

## 1. สร้าง LINE Login channel (ฟรี)

1. เข้า https://developers.line.biz/console/ (ล็อกอินด้วยบัญชี LINE)
2. สร้าง Provider (ชื่ออะไรก็ได้ เช่น "TripTogether")
3. ในนั้นสร้าง Channel ชนิด **LINE Login**
   - App type: **Web app** (browser flow ของ Firebase นับเป็น web)
4. จด **Channel ID** และ **Channel secret** (แท็บ Basic settings)

## 2. เปิด provider ใน Firebase console

โปรเจกต์ `triptogether-703c3` → Authentication → Sign-in method:

1. เปิด **Anonymous** (toggle เดียว — ใช้กับปุ่ม "เข้าใช้โดยไม่มีบัญชี")
2. Add new provider → **OpenID Connect**
   - ครั้งแรกจะบังคับ **Upgrade เป็น Identity Platform** — ฟรี (มี free tier ต่อ MAU) กดยืนยันได้
   - Grant type: **Code flow**
   - Name: `line` → **Provider ID ต้องเป็น `oidc.line` เป๊ะ ๆ** (โค้ดอ้างชื่อนี้)
   - Client ID: Channel ID จากข้อ 1
   - Issuer (URL): `https://access.line.me`
   - Client secret: Channel secret จากข้อ 1

## 3. ตั้ง callback URL ฝั่ง LINE

ใน LINE channel → แท็บ **LINE Login** → Callback URL:

```
https://triptogether-703c3.firebaseapp.com/__/auth/handler
```

## 4. ลงทะเบียน SHA-1 ของ signing key (จำเป็น!)

browser flow ของ Firebase (`startActivityForSignInWithProvider`) เช็ค cert hash ของแอป
ถ้าไม่ลงทะเบียน จะเจอ `INVALID_CERT_HASH 400` แล้ว login เด้งกลับทันที

Firebase console → Project settings (เฟือง) → Your apps → Android `com.triptogether` → **Add fingerprint**:

- debug keystore: `DF:B9:B8:BF:BB:F6:C1:6C:5B:B4:0B:5C:63:68:EF:78:27:4D:6B:41` (ลงทะเบียนไว้แล้วจากยุค Google sign-in)
- release keystore: `C4:C3:CE:4E:A3:D6:B2:D2:5D:B6:30:07:82:08:62:45:69:C8:A4:CB` (**ต้องเพิ่ม**)

## ปัญหาที่รู้จัก: บางเครื่อง Honor/Huawei

`FirebearCryptoHelper: KeysetManager failed to initialize` + `GenericIdpActivity: Could not generate an encryption key` — Firebase Auth สร้างกุญแจใน Android Keystore ไม่ได้ (บั๊กฝั่งเครื่อง)
ลอง: รีสตาร์ทเครื่องแล้วกดใหม่ / อัปเดต Google Play services. ถ้ายังไม่หาย ทางเลือกระยะยาวคือสลับไปใช้ LINE SDK ตรง (custom token ต้องมี Blaze)

## ทดสอบ

- Anonymous: ทดสอบได้ทันทีหลังเปิด toggle (ไม่เกี่ยวกับ LINE)
- LINE: กดปุ่มในแอป → เปิดเบราว์เซอร์ให้ล็อกอิน LINE → เด้งกลับแอปเป็นสถานะล็อกอิน
- หมายเหตุ: OIDC browser flow ไม่ใช้ SHA-1 — release keystore เปลี่ยนได้โดยไม่กระทบ login

## เชื่อมบัญชีทีหลัง (มีในแอปแล้ว)

ผู้ใช้ anonymous จะเห็นแถว "เชื่อมบัญชี LINE" ในตั้งค่า → `linkWithCredential` คง uid เดิม ทริปทั้งหมดติดตัวไป
