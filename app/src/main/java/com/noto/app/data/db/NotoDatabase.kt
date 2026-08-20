package com.noto.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.noto.app.data.entity.ChecklistItemEntity
import com.noto.app.data.entity.ProjectEntity
import com.noto.app.data.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, ProjectEntity::class, ChecklistItemEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class NotoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun checklistDao(): ChecklistDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `checklist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `taskId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_checklist_items_taskId` ON `checklist_items` (`taskId`)"
                )
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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
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
