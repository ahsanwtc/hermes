package pro.jsan.hermes.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import pro.jsan.hermes.data.SettingsRepository
import pro.jsan.hermes.data.db.SyncRuleDao
import pro.jsan.hermes.data.db.SyncedFileDao
import pro.jsan.hermes.data.model.SyncRule
import pro.jsan.hermes.worker.enqueueUpload
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {

    @Inject lateinit var syncRuleDao: SyncRuleDao
    @Inject lateinit var syncedFileDao: SyncedFileDao
    @Inject lateinit var settings: SettingsRepository

    private val observers = mutableMapOf<Int, ContentObserver>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        scope.launch {
            syncRuleDao.getEnabled().forEach { rule ->
                registerObserver(rule)
                catchUpScan(rule)
            }
        }
        return START_STICKY
    }

    private fun registerObserver(rule: SyncRule) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                uri?.let { scope.launch { handleNewFile(it, rule) } }
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"), true, observer
        )
        observers[rule.id] = observer
    }

    private suspend fun catchUpScan(rule: SyncRule) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?",
            arrayOf("${rule.localPath}%"),
            null
        ) ?: return

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val modCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val modifiedAt = it.getLong(modCol) * 1000
                val uri = MediaStore.Files.getContentUri("external", id)
                if (syncedFileDao.find(uri.toString(), modifiedAt) == null) {
                    enqueueUpload(this, uri, rule.id, modifiedAt, rule.cloudPath, isWifiOnly())
                }
            }
        }
    }

    private suspend fun handleNewFile(uri: Uri, rule: SyncRule) {
        val cursor = contentResolver.query(
            uri,
            arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED),
            null, null, null
        ) ?: return
        val modifiedAt = cursor.use {
            if (it.moveToFirst()) it.getLong(0) * 1000 else return
        }
        enqueueUpload(this, uri, rule.id, modifiedAt, rule.cloudPath, isWifiOnly())
    }

    private suspend fun isWifiOnly() = !settings.uploadOnMobileData.first()

    override fun onDestroy() {
        observers.values.forEach { contentResolver.unregisterContentObserver(it) }
        scope.cancel()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification() = run {
        val channelId = "sync_service"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Sync Service", NotificationManager.IMPORTANCE_LOW)
            )
        }
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hermes")
            .setContentText("Watching folders…")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1
    }
}
