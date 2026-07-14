package com.hjed.ottnavigator

object M3uParser {
    fun parse(content: String): List<Channel> {
        val channelList = mutableListOf<Channel>()
        var currentTitle = "Unknown Channel"
        var currentLogoUrl: String? = null
        var currentGroupTitle: String? = null
        var currentDrmKey: String? = null
        var currentDrmLicenseType: String? = null
        var currentManifestType: String? = null
        var currentHeaders = emptyMap<String, String>()

        content.lines().forEach { line ->
            val trimmed = line.trim()

            if (trimmed.startsWith("#EXTINF:")) {
                currentTitle = trimmed.substringAfterLast(",", "Unknown Channel")
                currentLogoUrl = trimmed.attributeValue("tvg-logo")
                currentGroupTitle = trimmed.attributeValue("group-title")
            } else if (trimmed.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                currentDrmKey = trimmed.substringAfter("license_key=")
            } else if (trimmed.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                currentDrmLicenseType = trimmed.substringAfter("license_type=")
            } else if (trimmed.startsWith("#KODIPROP:inputstream.adaptive.manifest_type=")) {
                currentManifestType = trimmed.substringAfter("manifest_type=")
            } else if (trimmed.startsWith("#KODIPROP:inputstream.adaptive.stream_headers=")) {
                val headersRaw = trimmed.substringAfter("stream_headers=")
                headersRaw.split("&").forEach { pair ->
                    val key = pair.substringBefore("=", "").trim()
                    val value = pair.substringAfter("=", "").trim()
                    if (key.isNotBlank() && value.isNotBlank()) {
                        currentHeaders = currentHeaders + (key to value)
                    }
                }
            } else if (trimmed.startsWith("#EXTVLCOPT:http-")) {
                val rawHeaderName = trimmed.substringAfter("#EXTVLCOPT:http-").substringBefore("=")
                val headerValue = trimmed.substringAfter("=", "")
                val headerName = rawHeaderName
                    .split("-")
                    .joinToString("-") { segment ->
                        segment.replaceFirstChar { char -> char.uppercaseChar() }
                    }
                    .let { if (it.equals("Referrer", ignoreCase = true)) "Referer" else it }

                if (headerName.isNotBlank() && headerValue.isNotBlank()) {
                    currentHeaders = currentHeaders + (headerName to headerValue)
                }
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                channelList.add(
                    Channel(
                        name = currentTitle,
                        url = trimmed,
                        logoUrl = currentLogoUrl,
                        groupTitle = currentGroupTitle,
                        group = currentGroupTitle,
                        manifestType = currentManifestType,
                        headers = currentHeaders,
                        drmLicenseType = currentDrmLicenseType,
                        drmLicenseKey = currentDrmKey
                    )
                )
                currentLogoUrl = null
                currentGroupTitle = null
                currentDrmKey = null
                currentDrmLicenseType = null
                currentManifestType = null
                currentHeaders = emptyMap()
            }
        }
        return channelList
    }

    private fun String.attributeValue(name: String): String? {
        val marker = "$name=\""
        return if (contains(marker)) substringAfter(marker).substringBefore("\"").takeIf { it.isNotBlank() } else null
    }

}
