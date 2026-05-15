package pro.jsan.hermes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synced_files")
data class SyncedFile(
    @PrimaryKey val localUri: String,
    val cloudFileId: String,
    val ruleId: Int,
    val uploadedAt: Long = System.currentTimeMillis(),
    val localModifiedAt: Long,
    val deleted: Boolean = false
)
