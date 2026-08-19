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

## ทดสอบ

- Anonymous: ทดสอบได้ทันทีหลังเปิด toggle (ไม่เกี่ยวกับ LINE)
- LINE: กดปุ่มในแอป → เปิดเบราว์เซอร์ให้ล็อกอิน LINE → เด้งกลับแอปเป็นสถานะล็อกอิน
- หมายเหตุ: OIDC browser flow ไม่ใช้ SHA-1 — release keystore เปลี่ยนได้โดยไม่กระทบ login

## เชื่อมบัญชีทีหลัง (มีในแอปแล้ว)

ผู้ใช้ anonymous จะเห็นแถว "เชื่อมบัญชี LINE" ในตั้งค่า → `linkWithCredential` คง uid เดิม ทริปทั้งหมดติดตัวไป
