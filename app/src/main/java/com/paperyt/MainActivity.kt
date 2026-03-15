@file:OptIn(ExperimentalMaterial3Api::class)
package com.paperyt

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.os.Build
import android.content.Intent
import com.paperyt.DownloadService


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        val repository = SettingsRepository(applicationContext)
        val vm: MainViewModel = ViewModelProvider(this, MainViewModel.factory(repository))[MainViewModel::class.java]

        handleShareIntent(intent, vm)

        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.factory(repository)
                )
                PaperYtApp(vm)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val vm: MainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        handleShareIntent(intent, vm)
    }

    private fun handleShareIntent(intent: Intent?, vm: MainViewModel) {

    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        if (sharedText.contains("http")) {

            vm.setSharedUrl(sharedText)

        }

    }


}

}

class MainViewModel(private val repository: SettingsRepository) : ViewModel() {
   var sharedUrl = mutableStateOf("")
        private set

    fun setSharedUrl(url: String) {
        sharedUrl.value = url
    }

    val settings: StateFlow<YtSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = YtSettings()
    )

    fun saveSettings(newSettings: YtSettings) {
        viewModelScope.launch {
            repository.update(newSettings)
        }
    }

    fun runDownload(url: String, preset: DownloadPreset, context: Context) {
    val fullCommand = buildCommand(url, preset)
    val args = fullCommand.split(" ").filter { it != "yt-dlp" }.toTypedArray()
    val dir = settings.value.downloadDirectory.ifBlank {
        context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    }
    val ffmpegPath = com.arthenica.ffmpegkit.FFmpegKitConfig.getFFmpegPath()

    val intent = Intent(context, DownloadService::class.java).apply {
        putExtra("url", url)
        putExtra("args", args)
        putExtra("dir", dir)
        putExtra("ffmpegPath", ffmpegPath)
    }
    context.startForegroundService(intent) // サービス開始！
}
    fun buildCommand(url: String, uiOption: DownloadPreset): String {
        val setting = settings.value
        val base = mutableListOf("yt-dlp")

        if (setting.customCommandEnabled && setting.customCommand.isNotBlank()) {
            base += setting.customCommand.trim().split(" ")
        } else {
            base += uiOption.toArgs(setting.outputExtension)
            if (setting.embedMetadata) base += "--embed-metadata"
            if (setting.useSponsorBlock) base += "--sponsorblock-remove all"
        }

        if (setting.downloadDirectory.isNotBlank()) {
            base += listOf("-P", setting.downloadDirectory)
        }

        base += url
        return base.joinToString(" ")
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}

@Composable
fun PaperYtApp(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current

    var url by rememberSaveable { mutableStateOf("") }
    
    var preset by rememberSaveable { mutableStateOf(DownloadPreset.BEST_VIDEO_AUDIO) }
    var commandPreview by rememberSaveable { mutableStateOf("") }
    
    val externalUrl by vm.sharedUrl
    LaunchedEffect(externalUrl) {
        if (externalUrl.isNotBlank()) {
            url = externalUrl
            vm.setSharedUrl("")
        }
    }

    LaunchedEffect(Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
        if (clipText.contains("youtube.com") || clipText.contains("youtu.be")) {
            url = clipText
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PaperYT (yt-dlp GUI)") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("URL確認", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("YouTube URL") }
            )

            PresetSelector(current = preset, onChange = { preset = it })

            Text("一時オプション", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = settings.outputExtension,
                onValueChange = {
                    vm.saveSettings(settings.copy(outputExtension = it.trim()))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("拡張子 (例: mp3 / mp4 / mkv)") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    commandPreview = vm.buildCommand(url, preset)
                }) { Text("コマンド確認") }
                Button(onClick = {
                    vm.runDownload(url, preset, context)
                }) { Text("ダウンロード開始") }
            }

            if (commandPreview.isNotBlank()) {
                Text("実行内容", style = MaterialTheme.typography.titleMedium)
                Text(commandPreview)
            }

            Spacer(Modifier.height(8.dp))
            SettingsSection(settings = settings, onSettingsChange = vm::saveSettings)

            Text(
                "今後: カスタムアイコン選択、共有メニュー連携、通知進捗、再試行キュー、署名済みReleaseの自動配布",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSelector(current: DownloadPreset, onChange: (DownloadPreset) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = current.label,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text("プリセット") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DownloadPreset.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(settings: YtSettings, onSettingsChange: (YtSettings) -> Unit) {
    Text("設定", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = settings.downloadDirectory,
        onValueChange = { onSettingsChange(settings.copy(downloadDirectory = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("ダウンロード先ディレクトリ") },
        enabled = settings.customCommandEnabled
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("カスタムコマンドを使う")
        Switch(
            checked = settings.customCommandEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(customCommandEnabled = it)) }
        )
    }

    OutlinedTextField(
        value = settings.customCommand,
        onValueChange = { onSettingsChange(settings.copy(customCommand = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("カスタムコマンド (yt-dlp以降) ") }
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("メタデータ埋め込み")
        Switch(
            checked = settings.embedMetadata,
            onCheckedChange = { onSettingsChange(settings.copy(embedMetadata = it)) }
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("SponsorBlock除去")
        Switch(
            checked = settings.useSponsorBlock,
            onCheckedChange = { onSettingsChange(settings.copy(useSponsorBlock = it)) }
        )
    }
}
