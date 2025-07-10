package ci.technchange.prestationscmu.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelperKt private constructor(context: Context) : SQLiteOpenHelper(context, "prestations_fse", null, 1) {

    companion object {
        @Volatile
        private var INSTANCE: DatabaseHelperKt? = null

        fun getInstance(context: Context): DatabaseHelperKt {
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseHelperKt(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = """
            CREATE TABLE IF NOT EXISTS utilisateurs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                phoneNumber TEXT UNIQUE,
                nom TEXT,
                prenom TEXT,
                centre TEXT
            );
        """.trimIndent()

        val createFingerprintTable = """
            CREATE TABLE IF NOT EXISTS empreintes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userId INTEGER,
                main TEXT,
                doigt TEXT,
                rawTemplate BLOB,
                FOREIGN KEY(userId) REFERENCES utilisateurs(id) ON DELETE CASCADE
            );
        """.trimIndent()

        db.execSQL(createUserTable)
        db.execSQL(createFingerprintTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS empreintes")
        db.execSQL("DROP TABLE IF EXISTS utilisateurs")
        onCreate(db)
    }
}
