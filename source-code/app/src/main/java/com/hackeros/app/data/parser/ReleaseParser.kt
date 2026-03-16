package com.hackeros.app.data.parser

import com.hackeros.app.data.model.ReleaseInfo

object ReleaseParser {

    fun parse(text: String): List<ReleaseInfo> {
        // 1. Clean outer brackets
        val cleanText = text.trim().removePrefix("[").removeSuffix("]").trim()

        val lines = cleanText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val releases = mutableListOf<ReleaseInfo>()

        var currentVersion: String? = null
        var currentSection: String? = null
        val tempNews = mutableListOf<String>()
        val tempEditions = mutableListOf<String>()
        val tempDescription = mutableListOf<String>()

        fun saveCurrentRelease() {
            currentVersion?.let { ver ->
                releases.add(
                    ReleaseInfo(
                        version = ver,
                        description = tempDescription.joinToString(" "),
                        news = tempNews.joinToString("\n"),
                        editions = tempEditions.joinToString("\n")
                    )
                )
            }
        }

        for (line in lines) {
            when {
                line.matches(Regex("^V\\d.*")) -> {
                    saveCurrentRelease()
                    currentVersion = line
                    currentSection = null
                    tempNews.clear()
                    tempEditions.clear()
                    tempDescription.clear()
                }
                line.lowercase().startsWith("new:") -> currentSection = "new"
                line.lowercase().startsWith("release:") -> currentSection = "release"
                line.lowercase().startsWith("description:") -> currentSection = "description"
                line.startsWith("=") -> {
                    val content = line.substring(1).trim()
                    when (currentSection) {
                        "new" -> tempNews.add(content)
                        "release" -> tempEditions.add(content)
                        "description" -> tempDescription.add(content)
                    }
                }
            }
        }
        saveCurrentRelease()
        return releases
    }
}
