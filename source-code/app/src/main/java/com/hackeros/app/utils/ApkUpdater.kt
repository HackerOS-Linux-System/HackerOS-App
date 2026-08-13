package com.hackeros.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a HackerOS App update APK with live progress, optionally verifies it against a
 * published SHA-256 checksum, and hands it off to the system Package Installer via a
 * FileProvider content:// Uri - the same "download with a progress bar, then act on the result"
 * pattern already used for wallpapers in [com.hackeros.app.ui.screens.WallpapersScreen].
 */
object ApkUpdater {

    sealed class VerifyResult {
        object Verified : VerifyResult()
        object ChecksumUnavailable : VerifyResult()
        object Mismatch : VerifyResult()
    }

    /**
     * Downloads [apkUrl] into the app's cache dir, reporting 0-100 integer progress via
     * [onProgress]. Returns the downloaded [File], or null on failure. [onProgress] may be
     * invoked from a background thread; callers updating Compose state directly from it is safe
     * since MutableState writes are snapshot-thread-safe.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        version: String,
        onProgress: (Int) -> Unit
    ): File? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                connect()
            }
            if (connection.responseCode !in 200..299) return null

            val contentLength = connection.contentLength
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clear any previously downloaded update APKs before writing the new one.
            dir.listFiles()?.forEach { it.delete() }
            val outFile = File(dir, "HackerOS-App-$version.apk")

            connection.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var totalRead = 0
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            val percent = ((totalRead.toLong() * 100) / contentLength).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            onProgress(100)
            outFile
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Verifies [file] against the SHA-256 checksum published at [checksumUrl]. If that URL
     * doesn't exist (a given release didn't publish one), verification is skipped rather than
     * blocking the update - the caller surfaces this distinction to the user instead of silently
     * treating "no checksum published" the same as "verified safe".
     */
    fun verifyChecksum(file: File, checksumUrl: String): VerifyResult {
        val remoteHex = try {
            val text = URL(checksumUrl).readText().trim()
            // Checksum files sometimes look like "<hash>  filename.apk" (sha256sum format);
            // only the first whitespace-separated token is the digest.
            text.split(Regex("\\s+")).firstOrNull()?.lowercase()
        } catch (_: Exception) {
            null
        } ?: return VerifyResult.ChecksumUnavailable

        if (!remoteHex.matches(Regex("^[0-9a-f]{64}$"))) return VerifyResult.ChecksumUnavailable

        val localHex = sha256Hex(file)
        return if (localHex.equals(remoteHex, ignoreCase = true)) VerifyResult.Verified else VerifyResult.Mismatch
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Launches the system Package Installer for [apkFile] via a FileProvider content Uri. */
    fun install(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
