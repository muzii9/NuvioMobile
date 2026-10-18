package com.nuvio.app.features.home

/**
 * Content-type rules shared by the future Movies and Series root screens.
 *
 * Use the catalog's declared type rather than its title: names like
 * "Animation", "Netflix" and "Trending" are not reliable type hints.
 * Unknown and live-TV types stay out of both browse sections.
 */
internal enum class SmartBrowseCategory {
    MOVIES,
    SERIES;

    fun matches(rawType: String): Boolean = when (this) {
        MOVIES -> rawType.trim().lowercase() in setOf("movie", "movies")
        SERIES -> rawType.trim().lowercase() in setOf("series", "tv", "show", "shows")
    }
}

/**
 * Keep only sections whose catalog request supports the selected category.
 * Preserve source order, targets and pagination so "View all" still opens
 * the original (type-specific) addon/server catalog.
 *
 * Individual preview items are also checked to prevent mixed metadata from
 * appearing under the wrong tab. Collection folders are excluded here;
 * their folder-level type needs separate handling.
 */
internal fun List<HomeCatalogSection>.forSmartBrowse(
    category: SmartBrowseCategory,
): List<HomeCatalogSection> = mapNotNull { section ->
    if (!category.matches(section.target.contentType)) return@mapNotNull null
    val items = section.items.filter { category.matches(it.type) }
    if (items.isEmpty()) return@mapNotNull null
    section.copy(items = items)
}
