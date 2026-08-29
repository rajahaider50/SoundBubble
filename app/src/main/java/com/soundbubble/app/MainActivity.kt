package com.soundbubble.app

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundbubble.app.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioDir: File
    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var adapter: AudioAdapter

    // ---- single file import ----
    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { copyImportedFile(it) }
        }

    // ---- whole folder import ----
    private val folderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                val count = FileImportHelper.importFolder(this, it, audioDir)
                Toast.makeText(this, "$count اڈیو فائلیں فولڈر سے شامل ہو گئیں ✅", Toast.LENGTH_SHORT).show()
                refreshList()
            }
        }

    // ---- zip import ----
    private val zipLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val count = FileImportHelper.importZip(this, it, audioDir)
                if (count > 0) {
                    Toast.makeText(this, "$count اڈیو فائلیں ZIP سے extract ہو گئیں ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "ZIP میں کوئی سپورٹڈ اڈیو نہیں ملی", Toast.LENGTH_SHORT).show()
                }
                refreshList()
            }
        }

    private val recordPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) toggleRecording()
            else Toast.makeText(this, "ریکارڈنگ کے لیے Mic Permission درکار ہے", Toast.LENGTH_SHORT).show()
        }

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val msg = if (granted) "بلوٹوتھ اجازت مل گئی ✅" else "بلوٹوتھ اجازت نہیں ملی"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

    private val overlaySettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay Permission مل گئی ✅", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioDir = File(getExternalFilesDir(null), "audio").apply { if (!exists()) mkdirs() }

        // ship-with-the-app default sounds get copied in on first run only
        FileImportHelper.copyDefaultSoundsIfNeeded(this, audioDir)

        adapter = AudioAdapter(getAudioFiles()) { file ->
            file.delete()
            refreshList()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnOverlayPermission.setOnClickListener { requestOverlayPermission() }

        binding.btnImportAudio.setOnClickListener {
            importLauncher.launch(arrayOf("audio/*"))
        }

        binding.btnImportFolder.setOnClickListener {
            folderLauncher.launch(null)
        }

        binding.btnImportZip.setOnClickListener {
            zipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
        }

        binding.btnRecordAudio.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            } else {
                toggleRecording()
            }
        }

        binding.btnBluetoothPermission.setOnClickListener { requestBluetoothPermission() }

        updateOutputModeButton()
        binding.btnOutputMode.setOnClickListener {
            AudioRouter.cycleMode(this)
            updateOutputModeButton()
        }

        binding.btnLaunch.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "پہلے Overlay Permission دیں", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
                return@setOnClickListener
            }
            if (getAudioFiles().isEmpty()) {
                Toast.makeText(this, "پہلے کم از کم ایک اڈیو شامل کریں", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, FloatingService::class.java)
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(
                this,
                "فلوٹنگ بٹن شروع ہو گیا۔ اب ہوم بٹن دبا کر گیم اوپن کریں",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun updateOutputModeButton() {
        binding.btnOutputMode.text = AudioRouter.modeLabel(AudioRouter.getMode(this))
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlaySettingsLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Overlay Permission پہلے سے موجود ہے", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                Toast.makeText(this, "بلوٹوتھ اجازت پہلے سے موجود ہے", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "اس Android ورژن پر اضافی اجازت درکار نہیں", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyImportedFile(uri: Uri) {
        try {
            val name = "imported_${System.currentTimeMillis()}.audio"
            val outFile = File(audioDir, name)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            Toast.makeText(this, "اڈیو شامل ہو گئی ✅", Toast.LENGTH_SHORT).show()
            refreshList()
        } catch (e: Exception) {
            Toast.makeText(this, "درآمد ناکام: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleRecording() {
        if (!isRecording) {
            val name = "rec_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a"
            val outFile = File(audioDir, name)
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.btnRecordAudio.text = "⏹ ریکارڈنگ روکیں اور محفوظ کریں"
        } else {
            try {
                recorder?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {
            }
            recorder = null
            isRecording = false
            binding.btnRecordAudio.text = "🎙 نئی اڈیو ریکارڈ کریں"
            Toast.makeText(this, "ریکارڈنگ محفوظ ہو گئی ✅", Toast.LENGTH_SHORT).show()
            refreshList()
        }
    }

    private fun getAudioFiles(): List<File> =
        audioDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    private fun refreshList() {
        adapter.updateData(getAudioFiles())
    }
}
