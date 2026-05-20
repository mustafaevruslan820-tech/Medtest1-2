package com.example.medtest1.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray

object MedicineCatalogImporter {

    private const val ASSET_NAME = "medicines_catalog.json"
    private const val TABLE = "medicine_catalog"

    fun importIfEmpty(context: Context, db: SQLiteDatabase) {
        db.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { c ->
            if (c.moveToFirst() && c.getInt(0) > 0) return
        }
        val text = context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val stmt = db.compileStatement(
            """
            INSERT INTO $TABLE (
                name_ru, name_lat, dosage, form, manufacturer, active_substance, aliases,
                indications, contraindications, side_effects, warnings
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        )
        db.beginTransaction()
        try {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val klass = o.optString("klass", "general_otc").ifBlank { "general_otc" }
                val m = MedicineCatalogMonographs.forClass(klass)
                val nameRu = o.optString("name_ru").ifBlank { continue }
                val nameLat = o.optString("name_lat").ifBlank { nameRu }
                val dosage = o.optString("dosage").ifBlank { "по инструкции" }
                val form = o.optString("form").ifBlank { "лекарственная форма по инструкции" }
                val manufacturer = o.optString("manufacturer").ifBlank { "различные производители" }
                val activeSubstance = o.optString("active_substance").ifBlank { nameLat }
                val aliases = o.optString("aliases")
                stmt.clearBindings()
                stmt.bindString(1, nameRu)
                stmt.bindString(2, nameLat)
                stmt.bindString(3, dosage)
                stmt.bindString(4, form)
                stmt.bindString(5, manufacturer)
                stmt.bindString(6, activeSubstance)
                stmt.bindString(7, aliases)
                stmt.bindString(8, m.indications)
                stmt.bindString(9, m.contraindications)
                stmt.bindString(10, m.sideEffects)
                stmt.bindString(11, m.warnings)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            stmt.close()
        }
    }
}
