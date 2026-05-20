package com.example.medtest1.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Patterns

data class UserProfile(
    val username: String,
    val photoUri: String,
    val fullName: String,
    val birthDate: String,
    val gender: String,
    val relativeContact: String,
    val bloodType: String,
    val chronicDiseases: String,
    val regularMedications: String,
    val weight: String,
    val height: String,
    val notificationMode: String,
    val customNotificationSound: String,
    val globalSilenceEnabled: Boolean,
    val badHabits: String,
    val allergies: String,
    val complexOperations: String
)

data class TreatmentPlan(
    val id: Long = 0L,
    val username: String,
    val medicineName: String,
    val dosage: String,
    val timesPerDay: Int,
    val reminderTime: String,
    val startDate: String,
    val endDate: String,
    val notes: String,
    /** Один курс заполнения дней — чтобы отделять завершённые курсы от нового расписания. */
    val courseId: Long = 0L
)

data class WellbeingEntry(
    val username: String,
    val date: String,
    val status: String,
    val comment: String
)

data class SavedMedicine(
    val id: Long = 0L,
    val username: String,
    val name: String,
    val dosage: String,
    val manufacturer: String,
    val activeSubstance: String,
    val useText: String,
    val sideEffects: String,
    val contraindications: String,
    val warnings: String,
    val imageUrl: String?,
    val sourceUrl: String?,
    val sourceSetId: String
)

data class UserAuthRecord(
    val username: String,
    val password: String,
    val email: String
)

data class ScannedMedicineCard(
    val id: Long = 0L,
    val username: String,
    val name: String,
    val barcode: String,
    val infoUrl: String,
    val updatedAt: Long = 0L
)

class AppDatabaseHelper(private val appContext: Context) :
    SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PROFILE (
                $COLUMN_PROFILE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PROFILE_USERNAME TEXT NOT NULL UNIQUE,
                $COLUMN_PHOTO_URI TEXT,
                $COLUMN_FULL_NAME TEXT,
                $COLUMN_BIRTH_DATE TEXT,
                $COLUMN_GENDER TEXT,
                $COLUMN_RELATIVE_CONTACT TEXT,
                $COLUMN_BLOOD_TYPE TEXT,
                $COLUMN_CHRONIC_DISEASES TEXT,
                $COLUMN_REGULAR_MEDICATIONS TEXT,
                $COLUMN_WEIGHT TEXT,
                $COLUMN_HEIGHT TEXT,
                $COLUMN_NOTIFICATION_MODE TEXT,
                $COLUMN_CUSTOM_NOTIFICATION_SOUND TEXT,
                $COLUMN_GLOBAL_SILENCE_ENABLED INTEGER DEFAULT 0,
                $COLUMN_BAD_HABITS TEXT,
                $COLUMN_ALLERGIES TEXT,
                $COLUMN_COMPLEX_OPERATIONS TEXT,
                FOREIGN KEY($COLUMN_PROFILE_USERNAME) REFERENCES $TABLE_USERS($COLUMN_USERNAME)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TREATMENTS (
                $COLUMN_TREATMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TREATMENT_USERNAME TEXT NOT NULL,
                $COLUMN_MEDICINE_NAME TEXT NOT NULL,
                $COLUMN_DOSAGE TEXT NOT NULL,
                $COLUMN_TIMES_PER_DAY INTEGER NOT NULL,
                $COLUMN_REMINDER_TIME TEXT NOT NULL,
                $COLUMN_START_DATE TEXT NOT NULL,
                $COLUMN_END_DATE TEXT NOT NULL,
                $COLUMN_NOTES TEXT,
                $COLUMN_TREATMENT_COURSE_ID INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY($COLUMN_TREATMENT_USERNAME) REFERENCES $TABLE_USERS($COLUMN_USERNAME)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_WELLBEING (
                $COLUMN_WELLBEING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WELLBEING_USERNAME TEXT NOT NULL,
                $COLUMN_WELLBEING_DATE TEXT NOT NULL,
                $COLUMN_WELLBEING_STATUS TEXT NOT NULL,
                $COLUMN_WELLBEING_COMMENT TEXT NOT NULL,
                UNIQUE($COLUMN_WELLBEING_USERNAME, $COLUMN_WELLBEING_DATE),
                FOREIGN KEY($COLUMN_WELLBEING_USERNAME) REFERENCES $TABLE_USERS($COLUMN_USERNAME)
            )
            """.trimIndent()
        )

        db.execSQL(CREATE_TABLE_SAVED_MEDICINES_SQL)
        db.execSQL(CREATE_TABLE_MEDICINE_CATALOG_SQL)
        db.execSQL(CREATE_TABLE_PASSWORD_RESET_CODES_SQL)
        db.execSQL(CREATE_TABLE_SCANNED_MEDICINE_CARDS_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 7) {
            db.execSQL(CREATE_TABLE_SAVED_MEDICINES_SQL)
        }
        if (oldVersion < 8) {
            db.execSQL(CREATE_TABLE_MEDICINE_CATALOG_SQL)
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_EMAIL TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                """
                UPDATE $TABLE_USERS
                SET $COLUMN_EMAIL = $COLUMN_USERNAME || '@example.local'
                WHERE $COLUMN_EMAIL = ''
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique ON $TABLE_USERS($COLUMN_EMAIL)"
            )
            db.execSQL(CREATE_TABLE_PASSWORD_RESET_CODES_SQL)
        }
        if (oldVersion < 10) {
            db.execSQL(CREATE_TABLE_SCANNED_MEDICINE_CARDS_SQL)
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE $TABLE_SCANNED_MEDICINE_CARDS RENAME TO ${TABLE_SCANNED_MEDICINE_CARDS}_old")
            db.execSQL(CREATE_TABLE_SCANNED_MEDICINE_CARDS_SQL)
            db.execSQL(
                """
                INSERT INTO $TABLE_SCANNED_MEDICINE_CARDS (
                    $COLUMN_SCAN_USERNAME,
                    $COLUMN_SCAN_NAME,
                    $COLUMN_SCAN_BARCODE,
                    $COLUMN_SCAN_INFO_URL,
                    $COLUMN_SCAN_UPDATED_AT
                )
                SELECT
                    $COLUMN_SCAN_USERNAME,
                    $COLUMN_SCAN_NAME,
                    $COLUMN_SCAN_BARCODE,
                    $COLUMN_SCAN_INFO_URL,
                    $COLUMN_SCAN_UPDATED_AT
                FROM ${TABLE_SCANNED_MEDICINE_CARDS}_old
                """.trimIndent()
            )
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_SCANNED_MEDICINE_CARDS}_old")
        }
        if (oldVersion < 12) {
            db.execSQL(
                "ALTER TABLE $TABLE_TREATMENTS ADD COLUMN $COLUMN_TREATMENT_COURSE_ID INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    fun registerUser(username: String, password: String, email: String): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_EMAIL, email)
        }
        return writableDatabase.insert(TABLE_USERS, null, values) != -1L
    }

    fun loginUser(username: String, password: String): Boolean {
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_ID FROM $TABLE_USERS
            WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?
            """.trimIndent(),
            arrayOf(username, password)
        ).use { cursor ->
            return cursor.count > 0
        }
    }

    fun getUsernameByEmail(email: String): String? {
        val normalizedEmail = email.trim().lowercase()
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_USERNAME FROM $TABLE_USERS
            WHERE lower($COLUMN_EMAIL) = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(normalizedEmail)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(0)?.trim()?.ifBlank { null }
        }
    }

    /** Если введённый логин/email и пароль совпадают с локальной таблицей users, возвращает канонический username. */
    fun verifyLocalCredentials(usernameOrEmail: String, password: String): String? {
        val id = usernameOrEmail.trim()
        if (id.isBlank() || password.isBlank()) return null
        val username = if (Patterns.EMAIL_ADDRESS.matcher(id).matches()) {
            getUsernameByEmail(id) ?: return null
        } else {
            id
        }
        return if (loginUser(username, password)) username else null
    }

    fun hasUsers(): Boolean {
        readableDatabase.rawQuery(
            "SELECT $COLUMN_ID FROM $TABLE_USERS LIMIT 1",
            null
        ).use { cursor ->
            return cursor.count > 0
        }
    }

    fun getAllUserAuthRecords(): List<UserAuthRecord> {
        val list = mutableListOf<UserAuthRecord>()
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_USERNAME, $COLUMN_PASSWORD, $COLUMN_EMAIL
            FROM $TABLE_USERS
            ORDER BY $COLUMN_USERNAME COLLATE NOCASE
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list += UserAuthRecord(
                    username = cursor.getString(0) ?: "",
                    password = cursor.getString(1) ?: "",
                    email = cursor.getString(2) ?: ""
                )
            }
        }
        return list
    }

    fun getEmailByUsername(username: String): String? {
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_EMAIL
            FROM $TABLE_USERS
            WHERE $COLUMN_USERNAME = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(username.trim())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(0)?.trim()?.ifBlank { null }
        }
    }

    fun isUserEmailPair(username: String, email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        return readableDatabase.rawQuery(
            """
            SELECT $COLUMN_ID
            FROM $TABLE_USERS
            WHERE $COLUMN_USERNAME = ? AND lower($COLUMN_EMAIL) = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(username.trim(), normalizedEmail)
        ).use { it.moveToFirst() }
    }

    fun createPasswordResetCode(username: String, email: String): String? {
        val normalizedEmail = email.trim().lowercase()
        val exists = readableDatabase.rawQuery(
            """
            SELECT $COLUMN_ID FROM $TABLE_USERS
            WHERE $COLUMN_USERNAME = ? AND lower($COLUMN_EMAIL) = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(username.trim(), normalizedEmail)
        ).use { it.moveToFirst() }
        if (!exists) return null

        val code = (100000..999999).random().toString()
        val expiresAt = System.currentTimeMillis() + (10 * 60 * 1000)
        val values = ContentValues().apply {
            put(COLUMN_RESET_USERNAME, username.trim())
            put(COLUMN_RESET_EMAIL, normalizedEmail)
            put(COLUMN_RESET_CODE, code)
            put(COLUMN_RESET_EXPIRES_AT, expiresAt)
        }
        writableDatabase.insertWithOnConflict(
            TABLE_PASSWORD_RESET_CODES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return code
    }

    fun updatePasswordByEmail(email: String, newPassword: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        return writableDatabase.update(
            TABLE_USERS,
            ContentValues().apply { put(COLUMN_PASSWORD, newPassword) },
            "lower($COLUMN_EMAIL) = ?",
            arrayOf(normalizedEmail)
        ) > 0
    }

    fun verifyResetCodeAndUpdatePassword(
        username: String,
        email: String,
        code: String,
        newPassword: String
    ): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val now = System.currentTimeMillis()
        val validCode = readableDatabase.rawQuery(
            """
            SELECT $COLUMN_RESET_USERNAME
            FROM $TABLE_PASSWORD_RESET_CODES
            WHERE $COLUMN_RESET_USERNAME = ?
              AND $COLUMN_RESET_EMAIL = ?
              AND $COLUMN_RESET_CODE = ?
              AND $COLUMN_RESET_EXPIRES_AT >= ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(username.trim(), normalizedEmail, code.trim(), now.toString())
        ).use { it.moveToFirst() }
        if (!validCode) return false

        val updated = writableDatabase.update(
            TABLE_USERS,
            ContentValues().apply { put(COLUMN_PASSWORD, newPassword) },
            "$COLUMN_USERNAME = ? AND lower($COLUMN_EMAIL) = ?",
            arrayOf(username.trim(), normalizedEmail)
        ) > 0
        if (!updated) return false

        writableDatabase.delete(
            TABLE_PASSWORD_RESET_CODES,
            "$COLUMN_RESET_USERNAME = ?",
            arrayOf(username.trim())
        )
        return true
    }

    fun upsertProfile(profile: UserProfile): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_PROFILE_USERNAME, profile.username)
            put(COLUMN_PHOTO_URI, profile.photoUri)
            put(COLUMN_FULL_NAME, profile.fullName)
            put(COLUMN_BIRTH_DATE, profile.birthDate)
            put(COLUMN_GENDER, profile.gender)
            put(COLUMN_RELATIVE_CONTACT, profile.relativeContact)
            put(COLUMN_BLOOD_TYPE, profile.bloodType)
            put(COLUMN_CHRONIC_DISEASES, profile.chronicDiseases)
            put(COLUMN_REGULAR_MEDICATIONS, profile.regularMedications)
            put(COLUMN_WEIGHT, profile.weight)
            put(COLUMN_HEIGHT, profile.height)
            put(COLUMN_NOTIFICATION_MODE, profile.notificationMode)
            put(COLUMN_CUSTOM_NOTIFICATION_SOUND, profile.customNotificationSound)
            put(COLUMN_GLOBAL_SILENCE_ENABLED, if (profile.globalSilenceEnabled) 1 else 0)
            put(COLUMN_BAD_HABITS, profile.badHabits)
            put(COLUMN_ALLERGIES, profile.allergies)
            put(COLUMN_COMPLEX_OPERATIONS, profile.complexOperations)
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_PROFILE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }

    fun getProfile(username: String): UserProfile? {
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_PHOTO_URI, $COLUMN_FULL_NAME, $COLUMN_BIRTH_DATE, $COLUMN_GENDER,
                   $COLUMN_RELATIVE_CONTACT, $COLUMN_BLOOD_TYPE, $COLUMN_CHRONIC_DISEASES,
                   $COLUMN_REGULAR_MEDICATIONS, $COLUMN_WEIGHT, $COLUMN_HEIGHT,
                   $COLUMN_NOTIFICATION_MODE, $COLUMN_CUSTOM_NOTIFICATION_SOUND,
                   $COLUMN_GLOBAL_SILENCE_ENABLED, $COLUMN_BAD_HABITS, $COLUMN_ALLERGIES,
                   $COLUMN_COMPLEX_OPERATIONS
            FROM $TABLE_PROFILE
            WHERE $COLUMN_PROFILE_USERNAME = ?
            """.trimIndent(),
            arrayOf(username)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return UserProfile(
                username = username,
                photoUri = cursor.getString(0) ?: "",
                fullName = cursor.getString(1) ?: "",
                birthDate = cursor.getString(2) ?: "",
                gender = cursor.getString(3) ?: "",
                relativeContact = cursor.getString(4) ?: "",
                bloodType = cursor.getString(5) ?: "",
                chronicDiseases = cursor.getString(6) ?: "",
                regularMedications = cursor.getString(7) ?: "",
                weight = cursor.getString(8) ?: "",
                height = cursor.getString(9) ?: "",
                notificationMode = cursor.getString(10) ?: "Системный звук",
                customNotificationSound = cursor.getString(11) ?: "",
                globalSilenceEnabled = cursor.getInt(12) == 1,
                badHabits = cursor.getString(13) ?: "",
                allergies = cursor.getString(14) ?: "",
                complexOperations = cursor.getString(15) ?: ""
            )
        }
    }

    fun addTreatmentPlan(plan: TreatmentPlan): Long {
        val values = ContentValues().apply {
            put(COLUMN_TREATMENT_USERNAME, plan.username)
            put(COLUMN_MEDICINE_NAME, plan.medicineName)
            put(COLUMN_DOSAGE, plan.dosage)
            put(COLUMN_TIMES_PER_DAY, plan.timesPerDay)
            put(COLUMN_REMINDER_TIME, plan.reminderTime)
            put(COLUMN_START_DATE, plan.startDate)
            put(COLUMN_END_DATE, plan.endDate)
            put(COLUMN_NOTES, plan.notes)
            put(COLUMN_TREATMENT_COURSE_ID, plan.courseId)
        }
        return writableDatabase.insert(TABLE_TREATMENTS, null, values)
    }

    fun getTreatmentPlans(username: String): List<TreatmentPlan> {
        val plans = mutableListOf<TreatmentPlan>()
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_TREATMENT_ID, $COLUMN_MEDICINE_NAME, $COLUMN_DOSAGE, $COLUMN_TIMES_PER_DAY,
                   $COLUMN_REMINDER_TIME, $COLUMN_START_DATE, $COLUMN_END_DATE, $COLUMN_NOTES,
                   $COLUMN_TREATMENT_COURSE_ID
            FROM $TABLE_TREATMENTS
            WHERE $COLUMN_TREATMENT_USERNAME = ?
            ORDER BY $COLUMN_TREATMENT_ID DESC
            """.trimIndent(),
            arrayOf(username)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                plans += TreatmentPlan(
                    id = cursor.getLong(0),
                    username = username,
                    medicineName = cursor.getString(1) ?: "",
                    dosage = cursor.getString(2) ?: "",
                    timesPerDay = cursor.getInt(3),
                    reminderTime = cursor.getString(4) ?: "08:00",
                    startDate = cursor.getString(5) ?: "",
                    endDate = cursor.getString(6) ?: "",
                    notes = cursor.getString(7) ?: "",
                    courseId = cursor.getLong(8)
                )
            }
        }
        return plans
    }

    fun deleteTreatmentPlan(planId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_TREATMENTS,
            "$COLUMN_TREATMENT_ID = ?",
            arrayOf(planId.toString())
        ) > 0
    }

    fun deleteTreatmentPlansForUser(username: String): Boolean {
        return writableDatabase.delete(
            TABLE_TREATMENTS,
            "$COLUMN_TREATMENT_USERNAME = ?",
            arrayOf(username)
        ) >= 0
    }

    fun upsertWellbeingEntry(username: String, date: String, status: String, comment: String): Boolean {
        val trimmedComment = comment.trim()
        if (trimmedComment.isBlank() && status.isBlank()) {
            return writableDatabase.delete(
                TABLE_WELLBEING,
                "$COLUMN_WELLBEING_USERNAME = ? AND $COLUMN_WELLBEING_DATE = ?",
                arrayOf(username, date)
            ) >= 0
        }
        val values = ContentValues().apply {
            put(COLUMN_WELLBEING_USERNAME, username)
            put(COLUMN_WELLBEING_DATE, date)
            put(COLUMN_WELLBEING_STATUS, status)
            put(COLUMN_WELLBEING_COMMENT, trimmedComment)
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_WELLBEING,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }

    fun getWellbeingEntries(username: String): Map<String, WellbeingEntry> {
        val result = linkedMapOf<String, WellbeingEntry>()
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_WELLBEING_DATE, $COLUMN_WELLBEING_STATUS, $COLUMN_WELLBEING_COMMENT
            FROM $TABLE_WELLBEING
            WHERE $COLUMN_WELLBEING_USERNAME = ?
            ORDER BY $COLUMN_WELLBEING_DATE DESC
            """.trimIndent(),
            arrayOf(username)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val day = cursor.getString(0)
                if (day != null) {
                    result[day] = WellbeingEntry(
                        username = username,
                        date = day,
                        status = cursor.getString(1) ?: "",
                        comment = cursor.getString(2) ?: ""
                    )
                }
            }
        }
        return result
    }

    fun insertSavedMedicine(medicine: SavedMedicine): Long {
        val values = ContentValues().apply {
            put(COLUMN_SAVED_USERNAME, medicine.username)
            put(COLUMN_SAVED_NAME, medicine.name)
            put(COLUMN_SAVED_DOSAGE, medicine.dosage)
            put(COLUMN_SAVED_MANUFACTURER, medicine.manufacturer)
            put(COLUMN_SAVED_ACTIVE_SUBSTANCE, medicine.activeSubstance)
            put(COLUMN_SAVED_USE_TEXT, medicine.useText)
            put(COLUMN_SAVED_SIDE_EFFECTS, medicine.sideEffects)
            put(COLUMN_SAVED_CONTRAINDICATIONS, medicine.contraindications)
            put(COLUMN_SAVED_WARNINGS, medicine.warnings)
            put(COLUMN_SAVED_IMAGE_URL, medicine.imageUrl)
            put(COLUMN_SAVED_SOURCE_URL, medicine.sourceUrl)
            put(COLUMN_SAVED_SOURCE_SET_ID, medicine.sourceSetId)
        }
        return writableDatabase.insert(TABLE_SAVED_MEDICINES, null, values)
    }

    fun getSavedMedicines(username: String): List<SavedMedicine> {
        val list = mutableListOf<SavedMedicine>()
        readableDatabase.rawQuery(
            """
            SELECT $COLUMN_SAVED_ID, $COLUMN_SAVED_NAME, $COLUMN_SAVED_DOSAGE, $COLUMN_SAVED_MANUFACTURER,
                   $COLUMN_SAVED_ACTIVE_SUBSTANCE, $COLUMN_SAVED_USE_TEXT, $COLUMN_SAVED_SIDE_EFFECTS,
                   $COLUMN_SAVED_CONTRAINDICATIONS, $COLUMN_SAVED_WARNINGS, $COLUMN_SAVED_IMAGE_URL,
                   $COLUMN_SAVED_SOURCE_URL, $COLUMN_SAVED_SOURCE_SET_ID
            FROM $TABLE_SAVED_MEDICINES
            WHERE $COLUMN_SAVED_USERNAME = ?
            ORDER BY $COLUMN_SAVED_ID DESC
            """.trimIndent(),
            arrayOf(username)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list += SavedMedicine(
                    id = cursor.getLong(0),
                    username = username,
                    name = cursor.getString(1) ?: "",
                    dosage = cursor.getString(2) ?: "",
                    manufacturer = cursor.getString(3) ?: "",
                    activeSubstance = cursor.getString(4) ?: "",
                    useText = cursor.getString(5) ?: "",
                    sideEffects = cursor.getString(6) ?: "",
                    contraindications = cursor.getString(7) ?: "",
                    warnings = cursor.getString(8) ?: "",
                    imageUrl = cursor.getString(9),
                    sourceUrl = cursor.getString(10),
                    sourceSetId = cursor.getString(11) ?: ""
                )
            }
        }
        return list
    }

    fun deleteSavedMedicine(id: Long): Boolean {
        return writableDatabase.delete(
            TABLE_SAVED_MEDICINES,
            "$COLUMN_SAVED_ID = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun upsertScannedMedicineCard(card: ScannedMedicineCard): Boolean {
        val values = ContentValues().apply {
            put(COLUMN_SCAN_USERNAME, card.username)
            put(COLUMN_SCAN_NAME, card.name)
            put(COLUMN_SCAN_BARCODE, card.barcode)
            put(COLUMN_SCAN_INFO_URL, card.infoUrl)
            put(COLUMN_SCAN_UPDATED_AT, System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict(
            TABLE_SCANNED_MEDICINE_CARDS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ) != -1L
    }

    fun getScannedMedicineCards(username: String): List<ScannedMedicineCard> {
        return readableDatabase.rawQuery(
            """
            SELECT $COLUMN_SCAN_ID, $COLUMN_SCAN_NAME, $COLUMN_SCAN_BARCODE, $COLUMN_SCAN_INFO_URL, $COLUMN_SCAN_UPDATED_AT
            FROM $TABLE_SCANNED_MEDICINE_CARDS
            WHERE $COLUMN_SCAN_USERNAME = ?
            ORDER BY $COLUMN_SCAN_UPDATED_AT DESC, $COLUMN_SCAN_ID DESC
            """.trimIndent(),
            arrayOf(username)
        ).use { cursor ->
            val result = mutableListOf<ScannedMedicineCard>()
            while (cursor.moveToNext()) {
                result += ScannedMedicineCard(
                    id = cursor.getLong(0),
                    username = username,
                    name = cursor.getString(1) ?: "",
                    barcode = cursor.getString(2) ?: "",
                    infoUrl = cursor.getString(3) ?: "",
                    updatedAt = cursor.getLong(4)
                )
            }
            result
        }
    }

    fun getScannedMedicineCard(username: String): ScannedMedicineCard? {
        return getScannedMedicineCards(username).firstOrNull()
    }

    fun deleteScannedMedicineCard(username: String, cardId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_SCANNED_MEDICINE_CARDS,
            "$COLUMN_SCAN_ID = ? AND $COLUMN_SCAN_USERNAME = ?",
            arrayOf(cardId.toString(), username)
        ) > 0
    }

    fun updateScannedMedicineCardName(username: String, cardId: Long, newName: String): Boolean {
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) return false
        return writableDatabase.update(
            TABLE_SCANNED_MEDICINE_CARDS,
            ContentValues().apply {
                put(COLUMN_SCAN_NAME, normalizedName)
                put(COLUMN_SCAN_UPDATED_AT, System.currentTimeMillis())
            },
            "$COLUMN_SCAN_ID = ? AND $COLUMN_SCAN_USERNAME = ?",
            arrayOf(cardId.toString(), username)
        ) > 0
    }

    fun deleteAccount(username: String): Boolean {
        val db = writableDatabase
        return try {
            db.beginTransaction()
            if (username.isBlank()) {
                db.setTransactionSuccessful()
                return true
            }
            db.delete(
                TABLE_SAVED_MEDICINES,
                "$COLUMN_SAVED_USERNAME = ?",
                arrayOf(username)
            )
            db.delete(
                TABLE_SCANNED_MEDICINE_CARDS,
                "$COLUMN_SCAN_USERNAME = ?",
                arrayOf(username)
            )
            db.delete(
                TABLE_WELLBEING,
                "$COLUMN_WELLBEING_USERNAME = ?",
                arrayOf(username)
            )
            db.delete(
                TABLE_TREATMENTS,
                "$COLUMN_TREATMENT_USERNAME = ?",
                arrayOf(username)
            )
            db.delete(
                TABLE_PROFILE,
                "$COLUMN_PROFILE_USERNAME = ?",
                arrayOf(username)
            )
            val usersDeleted = db.delete(
                TABLE_USERS,
                "$COLUMN_USERNAME = ?",
                arrayOf(username)
            )
            db.setTransactionSuccessful()
            // Идемпотентно: аккаунт считается удаленным, даже если запись уже отсутствовала.
            usersDeleted >= 0
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "medtest.db"
        private const val DATABASE_VERSION = 12

        private const val TABLE_USERS = "users"
        private const val TABLE_PROFILE = "profiles"
        private const val TABLE_TREATMENTS = "treatments"
        private const val TABLE_WELLBEING = "wellbeing_entries"
        private const val TABLE_SAVED_MEDICINES = "saved_medicines"
        private const val TABLE_PASSWORD_RESET_CODES = "password_reset_codes"
        private const val TABLE_SCANNED_MEDICINE_CARDS = "scanned_medicine_cards"

        private const val COLUMN_SAVED_ID = "saved_id"
        private const val COLUMN_SAVED_USERNAME = "saved_username"
        private const val COLUMN_SAVED_NAME = "saved_name"
        private const val COLUMN_SAVED_DOSAGE = "saved_dosage"
        private const val COLUMN_SAVED_MANUFACTURER = "saved_manufacturer"
        private const val COLUMN_SAVED_ACTIVE_SUBSTANCE = "saved_active_substance"
        private const val COLUMN_SAVED_USE_TEXT = "saved_use_text"
        private const val COLUMN_SAVED_SIDE_EFFECTS = "saved_side_effects"
        private const val COLUMN_SAVED_CONTRAINDICATIONS = "saved_contraindications"
        private const val COLUMN_SAVED_WARNINGS = "saved_warnings"
        private const val COLUMN_SAVED_IMAGE_URL = "saved_image_url"
        private const val COLUMN_SAVED_SOURCE_URL = "saved_source_url"
        private const val COLUMN_SAVED_SOURCE_SET_ID = "saved_source_set_id"

        private val CREATE_TABLE_MEDICINE_CATALOG_SQL = """
            CREATE TABLE IF NOT EXISTS medicine_catalog (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name_ru TEXT NOT NULL,
                name_lat TEXT NOT NULL,
                dosage TEXT NOT NULL,
                form TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                active_substance TEXT NOT NULL,
                aliases TEXT NOT NULL,
                indications TEXT NOT NULL,
                contraindications TEXT NOT NULL,
                side_effects TEXT NOT NULL,
                warnings TEXT NOT NULL
            )
        """.trimIndent()

        private val CREATE_TABLE_SAVED_MEDICINES_SQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_SAVED_MEDICINES (
                $COLUMN_SAVED_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SAVED_USERNAME TEXT NOT NULL,
                $COLUMN_SAVED_NAME TEXT NOT NULL,
                $COLUMN_SAVED_DOSAGE TEXT NOT NULL,
                $COLUMN_SAVED_MANUFACTURER TEXT NOT NULL,
                $COLUMN_SAVED_ACTIVE_SUBSTANCE TEXT NOT NULL,
                $COLUMN_SAVED_USE_TEXT TEXT NOT NULL,
                $COLUMN_SAVED_SIDE_EFFECTS TEXT NOT NULL,
                $COLUMN_SAVED_CONTRAINDICATIONS TEXT NOT NULL,
                $COLUMN_SAVED_WARNINGS TEXT NOT NULL,
                $COLUMN_SAVED_IMAGE_URL TEXT,
                $COLUMN_SAVED_SOURCE_URL TEXT,
                $COLUMN_SAVED_SOURCE_SET_ID TEXT NOT NULL
            )
        """.trimIndent()

        private const val COLUMN_SCAN_ID = "scan_id"
        private const val COLUMN_SCAN_USERNAME = "scan_username"
        private const val COLUMN_SCAN_NAME = "scan_name"
        private const val COLUMN_SCAN_BARCODE = "scan_barcode"
        private const val COLUMN_SCAN_INFO_URL = "scan_info_url"
        private const val COLUMN_SCAN_UPDATED_AT = "scan_updated_at"

        private val CREATE_TABLE_SCANNED_MEDICINE_CARDS_SQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_SCANNED_MEDICINE_CARDS (
                $COLUMN_SCAN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SCAN_USERNAME TEXT NOT NULL,
                $COLUMN_SCAN_NAME TEXT NOT NULL,
                $COLUMN_SCAN_BARCODE TEXT NOT NULL,
                $COLUMN_SCAN_INFO_URL TEXT NOT NULL,
                $COLUMN_SCAN_UPDATED_AT INTEGER NOT NULL,
                UNIQUE($COLUMN_SCAN_USERNAME, $COLUMN_SCAN_BARCODE)
            )
        """.trimIndent()

        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"

        private const val COLUMN_RESET_USERNAME = "reset_username"
        private const val COLUMN_RESET_EMAIL = "reset_email"
        private const val COLUMN_RESET_CODE = "reset_code"
        private const val COLUMN_RESET_EXPIRES_AT = "reset_expires_at"

        private val CREATE_TABLE_PASSWORD_RESET_CODES_SQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_PASSWORD_RESET_CODES (
                $COLUMN_RESET_USERNAME TEXT PRIMARY KEY,
                $COLUMN_RESET_EMAIL TEXT NOT NULL,
                $COLUMN_RESET_CODE TEXT NOT NULL,
                $COLUMN_RESET_EXPIRES_AT INTEGER NOT NULL
            )
        """.trimIndent()

        private const val COLUMN_PROFILE_ID = "profile_id"
        private const val COLUMN_PROFILE_USERNAME = "profile_username"
        private const val COLUMN_PHOTO_URI = "photo_uri"
        private const val COLUMN_FULL_NAME = "full_name"
        private const val COLUMN_BIRTH_DATE = "birth_date"
        private const val COLUMN_GENDER = "gender"
        private const val COLUMN_RELATIVE_CONTACT = "relative_contact"
        private const val COLUMN_BLOOD_TYPE = "blood_type"
        private const val COLUMN_CHRONIC_DISEASES = "chronic_diseases"
        private const val COLUMN_REGULAR_MEDICATIONS = "regular_medications"
        private const val COLUMN_WEIGHT = "weight"
        private const val COLUMN_HEIGHT = "height"
        private const val COLUMN_NOTIFICATION_MODE = "notification_mode"
        private const val COLUMN_CUSTOM_NOTIFICATION_SOUND = "custom_notification_sound"
        private const val COLUMN_GLOBAL_SILENCE_ENABLED = "global_silence_enabled"
        private const val COLUMN_BAD_HABITS = "bad_habits"
        private const val COLUMN_ALLERGIES = "allergies"
        private const val COLUMN_COMPLEX_OPERATIONS = "complex_operations"

        private const val COLUMN_TREATMENT_ID = "treatment_id"
        private const val COLUMN_TREATMENT_USERNAME = "treatment_username"
        private const val COLUMN_TREATMENT_COURSE_ID = "course_id"
        private const val COLUMN_MEDICINE_NAME = "medicine_name"
        private const val COLUMN_DOSAGE = "dosage"
        private const val COLUMN_TIMES_PER_DAY = "times_per_day"
        private const val COLUMN_REMINDER_TIME = "reminder_time"
        private const val COLUMN_START_DATE = "start_date"
        private const val COLUMN_END_DATE = "end_date"
        private const val COLUMN_NOTES = "notes"

        private const val COLUMN_WELLBEING_ID = "wellbeing_id"
        private const val COLUMN_WELLBEING_USERNAME = "wellbeing_username"
        private const val COLUMN_WELLBEING_DATE = "wellbeing_date"
        private const val COLUMN_WELLBEING_STATUS = "wellbeing_status"
        private const val COLUMN_WELLBEING_COMMENT = "wellbeing_comment"
    }
}
