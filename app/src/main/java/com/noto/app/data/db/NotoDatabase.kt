package com.noto.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.noto.app.data.entity.ProjectEntity
import com.noto.app.data.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, ProjectEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class NotoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: NotoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN calendarEventId INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'none'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN startDate TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER")
            }
        }

        fun getInstance(context: Context): NotoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): NotoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotoDatabase::class.java,
                "noto.db"
            )
                .addCallback(SeedCallback())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val defaults = listOf("Personal", "Work", "School", "Finance")
            defaults.forEach { name ->
                db.execSQL(
                    "INSERT INTO projects (name, colorHex, isDefault) VALUES (?, NULL, 1)",
                    arrayOf<Any?>(name)
                )
            }
        }
    }
}
