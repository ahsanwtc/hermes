package pro.jsan.hermes.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.FileObserver
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
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

    private val observers = mutableListOf<FileObserver>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        scope.launch {
            syncRuleDao.getEnabled().forEach { rule ->
                watchFolder(rule)
                catchUpScan(rule)
            }
        }
        return START_STICKY
    }

    private suspend fun watchFolder(rule: SyncRule) {
        val treeUri = Uri.parse(rule.localPath)
        val dir = DocumentFile.fromTreeUri(this, treeUri) ?: return
        // Resolve real filesystem path for FileObserver
        val path = getRealPath(treeUri) ?: return

        val observer = object : FileObserver(path, CREATE or CLOSE_WRITE) {
            override fun onEvent(event: Int, fileName: String?) {
                if (fileName == null || fileName.startsWith(".")) return
                scope.launch {
                    // Find the DocumentFile for this new file
                    val file = dir.findFile(fileName) ?: return@launch
                    val modifiedAt = file.lastModified()
                    if (syncedFileDao.find(file.uri.toString(), modifiedAt) == null) {
                        enqueueUpload(this@SyncService, file.uri, rule.id, modifiedAt, rule.cloudPath, isWifiOnly())
                    }
                }
            }
        }
        observer.startWatching()
        observers.add(observer)
    }

    private suspend fun catchUpScan(rule: SyncRule) {
        val treeUri = Uri.parse(rule.localPath)
        val dir = DocumentFile.fromTreeUri(this, treeUri) ?: return
        dir.listFiles().forEach { file ->
            if (!file.isFile) return@forEach
            val modifiedAt = file.lastModified()
            if (syncedFileDao.find(file.uri.toString(), modifiedAt) == null) {
                enqueueUpload(this, file.uri, rule.id, modifiedAt, rule.cloudPath, isWifiOnly())
            }
        }
    }

    /** Resolves a SAF tree URI to a real filesystem path for FileObserver */
    private fun getRealPath(treeUri: Uri): String? {
        val docId = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, treeUri)
            ?.uri?.lastPathSegment?.substringAfter(':') ?: return null
        return "${android.os.Environment.getExternalStorageDirectory().absolutePath}/$docId"
    }

    private suspend fun isWifiOnly() = !settings.uploadOnMobileData.first()

    override fun onDestroy() {
        observers.forEach { it.stopWatching() }
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
