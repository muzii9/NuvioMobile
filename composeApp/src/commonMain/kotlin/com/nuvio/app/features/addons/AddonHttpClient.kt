package com.nuvio.app.features.addons

internal suspend fun fetchAddonResponseText(
    url: String,
    forceRefresh: Boolean = false,
): String =
    if (forceRefresh) {
        httpGetTextWithHeaders(
            url = url,
            headers = mapOf(
                "Cache-Control" to "no-cache, no-store, max-age=0",
                "Pragma" to "no-cache",
            ),
        )
    } else {
        httpGetText(url)
    }
