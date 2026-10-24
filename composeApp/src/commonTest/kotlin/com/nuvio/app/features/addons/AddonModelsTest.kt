package com.nuvio.app.features.addons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours

class AddonModelsTest {

    @Test
    fun `addon response time formats milliseconds and seconds`() {
        assertEquals("1 ms", formatAddonResponseTime(0))
        assertEquals("95 ms", formatAddonResponseTime(95))
        assertEquals("555 ms", formatAddonResponseTime(555))
        assertEquals("999 ms", formatAddonResponseTime(999))
        assertEquals("1 s", formatAddonResponseTime(1_000))
        assertEquals("2.2 s", formatAddonResponseTime(2_200))
        assertEquals("2.3 s", formatAddonResponseTime(2_250))
        assertEquals("10 s", formatAddonResponseTime(9_999))
    }

    @Test
    fun `addon latency age displays human-readable freshness`() {
        assertEquals("just now", formatAddonLatencyAge(30.seconds))
        assertEquals("1m ago", formatAddonLatencyAge(1.minutes))
        assertEquals("59m ago", formatAddonLatencyAge(59.minutes))
        assertEquals("1h ago", formatAddonLatencyAge(61.minutes))
        assertEquals("2h ago", formatAddonLatencyAge(2.hours))
    }

    @Test
    fun `addon latency is marked stale after five minutes`() {
        assertFalse(isAddonLatencyStale(4.minutes))
        assertTrue(isAddonLatencyStale(5.minutes))
        assertTrue(isAddonLatencyStale(2.hours))
    }

    @Test
    fun `disabled addon is installed but not active`() {
        val addon = ManagedAddon(
            manifestUrl = "https://example.test/manifest.json",
            manifest = manifest(),
            enabled = false,
        )

        assertFalse(addon.isActive)
        assertEquals(0, listOf(addon).toOverview().activeAddons)
        assertEquals(0, listOf(addon).toOverview().totalCatalogs)
    }

    @Test
    fun `enabled addons helper filters disabled addons`() {
        val enabled = ManagedAddon(
            manifestUrl = "https://enabled.example/manifest.json",
            manifest = manifest(id = "enabled"),
            enabled = true,
        )
        val disabled = ManagedAddon(
            manifestUrl = "https://disabled.example/manifest.json",
            manifest = manifest(id = "disabled"),
            enabled = false,
        )

        assertEquals(listOf(enabled), listOf(enabled, disabled).enabledAddons())
        assertTrue(enabled.isActive)
    }

    @Test
    fun `pending manifest helpers only consider enabled unresolved addons`() {
        val pending = ManagedAddon(
            manifestUrl = "https://pending.example/manifest.json",
            isRefreshing = true,
        )
        val disabledPending = ManagedAddon(
            manifestUrl = "https://disabled.example/manifest.json",
            enabled = false,
            isRefreshing = true,
        )

        assertTrue(listOf(pending, disabledPending).hasPendingEnabledManifests())
        assertTrue(listOf(pending, disabledPending).isWaitingForFirstEnabledManifest())
        assertFalse(
            listOf(
                pending,
                ManagedAddon(
                    manifestUrl = "https://loaded.example/manifest.json",
                    manifest = manifest(id = "loaded"),
                ),
            ).isWaitingForFirstEnabledManifest(),
        )
    }

    @Test
    fun `manifest error helper ignores disabled and resolved addons`() {
        val disabledFailure = ManagedAddon(
            manifestUrl = "https://disabled.example/manifest.json",
            enabled = false,
            errorMessage = "Disabled failure",
        )
        val resolvedFailure = ManagedAddon(
            manifestUrl = "https://resolved.example/manifest.json",
            manifest = manifest(id = "resolved"),
            errorMessage = "Stale refresh failure",
        )
        val unresolvedFailure = ManagedAddon(
            manifestUrl = "https://failed.example/manifest.json",
            errorMessage = "Manifest failure",
        )

        assertEquals(
            "Manifest failure",
            listOf(disabledFailure, resolvedFailure, unresolvedFailure).firstEnabledManifestError(),
        )
    }
}

private fun manifest(id: String = "addon") = AddonManifest(
    id = id,
    name = id,
    description = "",
    version = "1.0.0",
    resources = listOf(AddonResource(name = "catalog", types = listOf("movie"))),
    types = listOf("movie"),
    catalogs = listOf(AddonCatalog(type = "movie", id = "popular", name = "Popular")),
    transportUrl = "https://$id.example/manifest.json",
)
