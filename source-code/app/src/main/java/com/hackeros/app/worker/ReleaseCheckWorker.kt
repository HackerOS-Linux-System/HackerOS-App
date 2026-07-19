package com.hackeros.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hackeros.app.Constants
import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ReleaseInfo
import com.hackeros.app.data.parser.ReleaseParser
import com.hackeros.app.data.parser.WebsiteReleaseParser
import com.hackeros.app.data.repository.PreferencesRepository
import com.hackeros.app.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Background release-check job, run periodically by [ReleaseNotificationScheduler].
 *
 * On every run it:
 *  1. Bails out early if the user has turned notifications off in the meantime.
 *  2. Fetches the latest releases from the official website (same source used by the
 *     Releases screen), falling back to the legacy `.hacker` file if the site is unreachable.
 *  3. Compares the newest release's version against the last version the user has seen
 *     (either in-app or via a previous notification).
 *  4. Posts a localized notification only if a genuinely new version was found, then
 *     records it as the new "last known" version so the same release is never announced twice.
 */
class ReleaseCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesRepository(applicationContext)

        return try {
            val notificationsEnabled = prefs.notificationsFlow.first()
            if (!notificationsEnabled) return Result.success()

            val language = prefs.languageFlow.first()
            val releases = fetchLatestReleases(language)
            val latest = releases.firstOrNull() ?: return Result.retry()

            val lastKnown = prefs.lastKnownVersionFlow.first()
            if (lastKnown != null && lastKnown != latest.version) {
                NotificationHelper.showNewReleaseNotification(applicationContext, latest, language)
            }
            prefs.saveLastKnownVersion(latest.version)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchLatestReleases(language: Language): List<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            try {
                val text = URL("${Constants.WEBSITE_RELEASES_JS_URL}?t=${System.currentTimeMillis()}")
                    .readText()
                val parsed = WebsiteReleaseParser.parse(text, language)
                if (parsed.isNotEmpty()) return@withContext parsed
            } catch (_: Exception) {
                // fall through to legacy source
            }
            try {
                val legacyText = URL("${Constants.LEGACY_RELEASE_INFO_URL}?t=${System.currentTimeMillis()}")
                    .readText()
                return@withContext ReleaseParser.parse(legacyText)
            } catch (_: Exception) {
                emptyList()
            }
        }
}
