package pro.jsan.hermes.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val httpClient: HttpClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var url by mutableStateOf("")
    var folderUri by mutableStateOf<Uri?>(null)
    var status by mutableStateOf("")
    var isDownloading by mutableStateOf(false)

    fun onFolderPicked(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        folderUri = uri
    }

    fun download() = viewModelScope.launch {
        val uri = folderUri ?: run { status = "Please select a folder"; return@launch }
        if (url.isBlank()) { status = "Please enter a URL"; return@launch }
        isDownloading = true
        status = "Downloading…"
        runCatching {
            val bytes = httpClient.get(url).bodyAsBytes()
            if (bytes.size < 100 || String(bytes.take(100).toByteArray()).contains("<html", ignoreCase = true)) {
                error("URL returned a web page, not a file. Try copying the direct file URL.")
            }
            val fileName = url.substringAfterLast('/').substringBefore('?').ifBlank { "download" }
            val mimeType = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(fileName.substringAfterLast('.', ""))
                ?: "application/octet-stream"
            val dir = DocumentFile.fromTreeUri(context, uri) ?: error("Cannot access folder")
            val file = dir.createFile(mimeType, fileName) ?: error("Cannot create file")
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
                ?: error("Cannot write file")
            status = "✓ Saved $fileName"
            url = ""
        }.onFailure {
            status = "✗ ${it.message ?: "Download failed"}"
        }
        isDownloading = false
    }
}
