# 05 — Architecture

## Gradle modules

แยกโมดูลตั้งแต่วันแรกเพื่อให้ย้ายไป KMP ตอนทำ iOS ได้โดยไม่ต้องรื้อ

```
:app                    Application, MainActivity, NavHost, DI wiring
:core:domain            models, use cases, repository interfaces, money logic
                        ← ไม่มี dependency กับ Android หรือ Firebase เลย
:core:data              Firestore/Storage/Auth implementations ของ repository
:core:ui                theme, design tokens, Composable ที่ใช้ร่วมกัน
:core:testing           test fixtures, fake repositories, data builders
:feature:auth           S01, S14
:feature:trip           S02, S03, S04, S05
:feature:plan           S06, S07
:feature:expense        S08, S09, S10
:feature:settlement     S11
:feature:extras         S12, S13
```

**กฎ dependency:** `feature:*` → `core:domain` + `core:ui` เท่านั้น
ห้าม `feature` ใดขึ้นกับ `feature` อื่น ห้าม `feature` ขึ้นกับ `core:data` โดยตรง
`:app` เป็นที่เดียวที่ผูก `core:data` เข้ากับ interface ใน `core:domain`

บังคับกฎนี้ด้วย [Konsist](https://docs.konsist.lemonappdev.com/) หรือ custom Gradle check ใน CI

## Package layout ต่อ feature

```
feature/expense/
├── ExpenseListScreen.kt        Composable + @Preview
├── ExpenseListViewModel.kt     @HiltViewModel
├── ExpenseListUiState.kt       data class + sealed interface Event
├── ExpenseEditorScreen.kt
├── ExpenseEditorViewModel.kt
├── ExpenseEditorUiState.kt
├── components/                 Composable ย่อยเฉพาะ feature นี้
└── navigation/ExpenseNav.kt    route แบบ type-safe + NavGraphBuilder ext
```

## รูปแบบ state

```kotlin
data class ExpenseEditorUiState(
    val title: String = "",
    val totalInput: String = "",
    val splitMode: SplitMode = SplitMode.EQUAL,
    val members: List<MemberRow> = emptyList(),
    val delta: Money = Money.ZERO,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean
        get() = title.isNotBlank() && delta.isZero && members.any { it.selected } && !isSaving
}

sealed interface ExpenseEditorEvent {
    data object Saved : ExpenseEditorEvent
    data class Error(val messageResId: Int) : ExpenseEditorEvent
}
```

- state ตัวเดียวต่อหน้าจอ expose เป็น `StateFlow`
- one-shot event ผ่าน `Channel(BUFFERED).receiveAsFlow()` ไม่ใช่ใส่ใน state
- **logic คำนวณอยู่ใน `core:domain` เสมอ** ViewModel แค่เรียกใช้แล้วแปลงเป็น state

## Repository interfaces (อยู่ใน `core:domain`)

```kotlin
interface TripRepository {
    fun observeTrips(userId: String): Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>
    suspend fun createTrip(draft: TripDraft): Result<String>
    suspend fun joinByCode(code: String, userId: String): Result<String>
}

interface ExpenseRepository {
    fun observeExpenses(tripId: String): Flow<List<Expense>>
    suspend fun upsert(tripId: String, expense: Expense): Result<Unit>
    suspend fun delete(tripId: String, expenseId: String): Result<Unit>
    suspend fun uploadSlip(tripId: String, expenseId: String, bytes: ByteArray): Result<String>
}
```

ทุก method คืน `Result<T>` ไม่ throw ข้าม layer

## Firestore ↔ domain mapping

- DTO แยกจาก domain model อยู่ใน `core:data/dto/`
- mapper เป็น extension function `ExpenseDto.toDomain()` / `Expense.toDto()`
- **เงินใน Firestore เป็น number (Long สตางค์)** mapper แปลงเป็น `Money` ห้ามส่ง `Money` เข้า Firestore ตรง ๆ
- วันที่เก็บเป็น string `yyyy-MM-dd` mapper แปลงเป็น `LocalDate`

## Offline

เปิด Firestore persistence ตอน init:

```kotlin
FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
    setLocalCacheSettings(persistentCacheSettings {})
}
```

จากนั้น write ทุกตัวจะ optimistic ให้เอง ไม่ต้องสร้าง cache layer ซ้อน **อย่าเพิ่ม Room ใน v1**

## Cloud Functions (TypeScript)

| function | trigger | หน้าที่ |
|---|---|---|
| `onExpenseWrite` | Firestore write ที่ `expenses/{id}` | ตรวจ invariant, ส่ง FCM ให้สมาชิกคนอื่น |
| `onSettlementCreate` | create ที่ `settlements/{id}` | ส่ง FCM ให้ผู้รับเงินไปกดยืนยัน |
| `onActivityWrite` | write ที่ `activities/{id}` | ส่ง FCM แจ้งแผนเปลี่ยน (debounce 5 นาที) |
| `cleanupExpiredInvites` | scheduled รายวัน | ปิด invite code ที่หมดอายุ |
| `onTripDelete` | delete ที่ `trips/{id}` | ลบ subcollection และไฟล์ใน Storage |

FCM payload ต้องมี `tripId` และ `screen` เพื่อให้แตะแล้วเปิดตรงหน้า

## Testing

| ชั้น | เครื่องมือ | ครอบคลุมอะไร |
|---|---|---|
| domain | JUnit5 + property test | **money logic ทั้งหมด — บังคับ 100%** |
| ViewModel | Turbine + fake repository | state transition, validation, error |
| Repository | Firestore emulator | mapping, query, rules |
| UI | Compose UI Test | ExpenseEditor flow เป็นอย่างน้อย |

`core:testing` มี `FakeExpenseRepository`, `TripBuilder`, `MemberBuilder` ให้เทสต์ทุกโมดูลใช้ร่วมกัน

## CI (GitHub Actions)

```
on PR:  ktlintCheck → detekt → test → assembleDebug → module dependency check
on main: + Firebase App Distribution ให้กลุ่ม internal
```

## เตรียมทางไป iOS

`core:domain` ห้ามมี Android/Firebase import — บังคับใน CI ด้วย script เช็ค import
เมื่อจะทำ iOS: เปลี่ยน `:core:domain` เป็น KMP module, ใช้ `kotlinx-datetime` (ใช้อยู่แล้ว) แล้ว logic การเงินทั้งหมดข้ามไปได้ทันที เขียนใหม่แค่ UI (SwiftUI) กับ data layer
