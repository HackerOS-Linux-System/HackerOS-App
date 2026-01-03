package com.hackeros.hackeros-app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.hackeros.app.databinding.ActivityMainBinding
import pl.hackeros.app.databinding.ItemWallpaperBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val wallpaperUrls = listOf(
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/default-wallpaper.png",
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper.png",
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper1.png",
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper2.png",
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper3.png",
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper4.png",
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper5.png"
    )

    private val releaseInfoUrl =
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-App/main/release-info.hacker"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tapety
        binding.recyclerWallpapers.adapter = WallpaperAdapter(wallpaperUrls, this)
        binding.recyclerWallpapers.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        // Informacje o wydaniach
        loadReleasesInfo()
    }

    private fun loadReleasesInfo() {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(releaseInfoUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val content = response.body!!.string()

                    val lines = content.lines()
                    val allText = content.trim()
                    val latest = lines.takeLastWhile { it.isNotBlank() }.joinToString("\n")

                    runOnUiThread {
                        binding.tvLatestRelease.text = latest.ifBlank { "Brak informacji o najnowszym wydaniu" }
                        binding.tvAllReleases.text = allText.ifBlank { "Brak informacji o wydaniach" }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    val errorMsg = getString(R.string.error_occurred, e.localizedMessage ?: "Nieznany błąd")
                    binding.tvLatestRelease.text = errorMsg
                    binding.tvAllReleases.text = errorMsg
                }
            }
        }.start()
    }

    fun downloadImage(url: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("HackerOS Wallpaper")
                .setDescription("Pobieranie tapety z HackerOS")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, url.substringAfterLast("/"))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(this, "Rozpoczęto pobieranie...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Nie udało się rozpocząć pobierania", Toast.LENGTH_LONG).show()
        }
    }
}
