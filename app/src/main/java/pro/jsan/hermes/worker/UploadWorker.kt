package pro.jsan.hermes.worker

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pro.jsan.hermes.data.FilenApiClient
import pro.jsan.hermes.data.db.SyncRuleDao
import pro.jsan.hermes.data.db.SyncedFileDao
import pro.jsan.hermes.data.model.SyncedFile
import java.util.concurrent.TimeUnit

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val api: FilenApiClient,
    private val syncedFileDao: SyncedFileDao,
    private val syncRuleDao: SyncRuleDao
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString("uri") ?: return Result.failure()
        val ruleId = inputData.getInt("ruleId", -1)
        val modifiedAt = inputData.getLong("modifiedAt", 0)
        val cloudPath = inputData.getString("cloudPath") ?: return Result.failure()

        val uri = Uri.parse(uriStr)

        // Skip if already synced at this modifiedAt
        if (syncedFileDao.find(uriStr, modifiedAt) != null) return Result.success()

        // Read file bytes
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.retry()

        val name = uri.lastPathSegment ?: "file"
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(name.substringAfterLast('.', ""))
            ?: "application/octet-stream"

        // Resolve cloud directory and upload
        val parentUuid = api.resolveCloudPath(cloudPath)
        val cloudFileId = api.uploadFile(name, parentUuid, bytes, mimeType, modifiedAt)

        syncedFileDao.upsert(SyncedFile(
            localUri = uriStr,
            cloudFileId = cloudFileId,
            ruleId = ruleId,
            localModifiedAt = modifiedAt
        ))

        // Delete local file if rule says so and extension not in retain filter
        val rule = syncRuleDao.getEnabled().find { it.id == ruleId }
        if (rule?.deleteAfterUpload == true) {
            val ext = name.substringAfterLast('.', "").lowercase()
            val retained = rule.retainFilter?.split(',')?.map { it.trim().lowercase() } ?: emptyList()
            if (ext !in retained) {
                ctx.contentResolver.delete(uri, null, null)
            }
        }

        return Result.success()
    }
}

fun enqueueUpload(
    context: Context,
    uri: Uri,
    ruleId: Int,
    modifiedAt: Long,
    cloudPath: String,
    wifiOnly: Boolean
) {
    val constraints = Constraints(
        requiredNetworkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
    )
    val request = OneTimeWorkRequestBuilder<UploadWorker>()
        .setConstraints(constraints)
        .setInputData(workDataOf(
            "uri" to uri.toString(),
            "ruleId" to ruleId,
            "modifiedAt" to modifiedAt,
            "cloudPath" to cloudPath
        ))
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        uri.toString(), ExistingWorkPolicy.KEEP, request
    )
}
