package com.soundbubble.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object FileImportHelper {

    private val AUDIO_EXT = setOf("mp3", "wav", "m4a", "ogg", "aac", "3gp", "amr")

    private fun isAudioFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in AUDIO_EXT
    }

    /** Recursively copies every audio file found under a picked folder (SAF tree) into [targetDir].
     *  Returns how many files were copied. */
    fun importFolder(context: Context, treeUri: Uri, targetDir: File): Int {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return 0
        var count = 0

        fun walk(doc: DocumentFile, prefix: String) {
            doc.listFiles().forEach { child ->
                val childName = child.name ?: return@forEach
                if (child.isDirectory) {
                    walk(child, "${prefix}${childName}_")
                } else if (isAudioFile(childName)) {
                    try {
                        val outFile = File(targetDir, "folder_${prefix}${childName}")
                        context.contentResolver.openInputStream(child.uri)?.use { input ->
                            FileOutputStream(outFile).use { output -> input.copyTo(output) }
                        }
                        count++
                    } catch (_: Exception) {
                    }
                }
            }
        }
        walk(root, "")
        return count
    }

    /** Extracts every audio entry inside a picked .zip file into [targetDir] (folders inside the
     *  zip are flattened, filenames are kept unique). Returns how many files were extracted. */
    fun importZip(context: Context, zipUri: Uri, targetDir: File): Int {
        var count = 0
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && isAudioFile(name)) {
                        val flatName = name.substringAfterLast('/')
                        val outFile = File(targetDir, "zip_${System.nanoTime()}_$flatName")
                        FileOutputStream(outFile).use { output -> zis.copyTo(output) }
                        count++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return count
    }

    /** Copies whatever audio files are bundled in assets/default_sounds into [targetDir], once per
     *  install. This is how developers ship "default" sounds that every new user already has. */
    fun copyDefaultSoundsIfNeeded(context: Context, targetDir: File) {
        val prefs = context.getSharedPreferences("soundbubble_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("defaults_copied", false)) return

        try {
            val assetFiles = context.assets.list("default_sounds") ?: emptyArray()
            for (name in assetFiles) {
                if (!isAudioFile(name)) continue
                val outFile = File(targetDir, "default_$name")
                if (outFile.exists()) continue
                context.assets.open("default_sounds/$name").use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
            }
        } catch (_: Exception) {
            // no default_sounds folder or nothing inside it -- fine, just skip
        }

        prefs.edit().putBoolean("defaults_copied", true).apply()
    }
}
