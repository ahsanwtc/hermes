package pro.jsan.hermes.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pro.jsan.hermes.data.model.SyncRule
import pro.jsan.hermes.data.model.SyncedFile

@Database(entities = [SyncRule::class, SyncedFile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncRuleDao(): SyncRuleDao
    abstract fun syncedFileDao(): SyncedFileDao
}
