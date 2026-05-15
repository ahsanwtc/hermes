package pro.jsan.hermes.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_rules")
data class SyncRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localPath: String,
    val cloudPath: String,
    val deleteAfterUpload: Boolean = false,
    val retainFilter: String? = null,
    val enabled: Boolean = true
)
