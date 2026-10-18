package com.nuvio.app.features.home

import com.nuvio.app.features.catalog.CatalogTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartBrowseCategoryTest {
    private fun section(
        type: String,
        items: List<MetaPreview>,
        key: String = type,
    ) = HomeCatalogSection(
        key = key,
        title = "Trending",
        subtitle = "Addon",
        addonName = "Addon",
        target = CatalogTarget.Addon(
            manifestUrl = "https://example.test/manifest.json",
            contentType = type,
            catalogId = "trending",
            supportsPagination = true,
        ),
        items = items,
        availableItemCount = 50,
        hasMore = true,
    )

    private fun preview(id: String, type: String) = MetaPreview(
        id = id,
        type = type,
        name = id,
    )

    @Test
    fun moviesOnlyIncludesMovieCatalogs() {
        val sections = listOf(
            section("movie", listOf(preview("m1", "movie"))),
            section("series", listOf(preview("s1", "series"))),
            section("live", listOf(preview("channel", "live"))),
        )
        assertEquals(listOf("movie"), sections.forSmartBrowse(SmartBrowseCategory.MOVIES).map { it.key })
    }

    @Test
    fun seriesSupportsTvAliasAndCanonicalSeries() {
        val sections = listOf(
            section("tv", listOf(preview("s1", "tv"))),
            section("series", listOf(preview("s2", "series"))),
            section("movie", listOf(preview("m1", "movie"))),
        )
        assertEquals(listOf("tv", "series"), sections.forSmartBrowse(SmartBrowseCategory.SERIES).map { it.key })
    }

    @Test
    fun filtersMisclassifiedItemsWithoutChangingCatalogTarget() {
        val original = section("movie", listOf(preview("m1", "movie"), preview("s1", "series")))
        val result = listOf(original).forSmartBrowse(SmartBrowseCategory.MOVIES).single()
        assertEquals(listOf("m1"), result.items.map { it.id })
        assertEquals(original.target, result.target)
        assertEquals(50, result.availableItemCount)
        assertTrue(result.hasMore)
    }

    @Test
    fun unknownCatalogTypeIsNotGuessedFromTitlesOrItems() {
        val unknown = section("other", listOf(preview("m1", "movie")))
        assertTrue(listOf(unknown).forSmartBrowse(SmartBrowseCategory.MOVIES).isEmpty())
    }

    @Test
    fun removesEmptySectionsAndPreservesOriginalOrdering() {
        val input = listOf(
            section("movie", listOf(preview("a", "movie")), key = "first"),
            section("movie", listOf(preview("wrong", "series")), key = "excluded"),
            section("movies", listOf(preview("b", "movies")), key = "last"),
        )
        assertEquals(listOf("first", "last"), input.forSmartBrowse(SmartBrowseCategory.MOVIES).map { it.key })
    }

    @Test
    fun categoryNamesIgnoreWhitespaceAndCase() {
        assertTrue(SmartBrowseCategory.MOVIES.matches(" MOVIE "))
        assertTrue(SmartBrowseCategory.SERIES.matches(" TV "))
    }
}
