package pro.jsan.hermes.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pro.jsan.hermes.data.model.SyncedFile

@Dao
interface SyncedFileDao {
    @Query("SELECT * FROM synced_files WHERE localUri = :uri AND localModifiedAt = :modifiedAt")
    suspend fun find(uri: String, modifiedAt: Long): SyncedFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: SyncedFile)

    @Query("SELECT * FROM synced_files ORDER BY uploadedAt DESC LIMIT 100")
    fun getRecent(): Flow<List<SyncedFile>>
}
