package com.hjed.ottnavigator

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val manifestType: String? = null,
    val drmLicenseType: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val drmLicenseKey: String? = null
)
