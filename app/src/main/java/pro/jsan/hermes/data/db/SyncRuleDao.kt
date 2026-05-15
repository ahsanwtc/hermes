package pro.jsan.hermes.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pro.jsan.hermes.data.model.SyncRule

@Dao
interface SyncRuleDao {
    @Query("SELECT * FROM sync_rules")
    fun getAll(): Flow<List<SyncRule>>

    @Query("SELECT * FROM sync_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<SyncRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: SyncRule)

    @Delete
    suspend fun delete(rule: SyncRule)
}
