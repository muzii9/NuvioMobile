package com.nuvio.app.features.addons

import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.generic_addon
import org.jetbrains.compose.resources.getString

data class AddonManifest(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val logoUrl: String? = null,
    val resources: List<AddonResource>,
    val types: List<String>,
    val idPrefixes: List<String> = emptyList(),
    val catalogs: List<AddonCatalog> = emptyList(),
    val behaviorHints: AddonBehaviorHints = AddonBehaviorHints(),
    val transportUrl: String,
)

data class AddonResource(
    val name: String,
    val types: List<String>,
    val idPrefixes: List<String> = emptyList(),
)

data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<AddonExtraProperty> = emptyList(),
)

data class AddonExtraProperty(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
    val optionsLimit: Int? = null,
)

data class AddonBehaviorHints(
    val configurable: Boolean = false,
    val configurationRequired: Boolean = false,
    val adult: Boolean = false,
    val p2p: Boolean = false,
)

data class ManagedAddon(
    val manifestUrl: String,
    val manifest: AddonManifest? = null,
    val userSetName: String? = null,
    val enabled: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val responseTimeMs: Long? = null,
    val responseCheckedAt: TimeMark? = null,
) {
    val isActive: Boolean
        get() = enabled && manifest != null

    val displayTitle: String
        get() = userSetName?.takeIf { it.isNotBlank() && it != manifest?.name }
            ?: manifest?.name
            ?: manifestUrl.substringBefore("?").substringAfterLast("/").ifBlank {
                runBlocking { getString(Res.string.generic_addon) }
            }
}

internal fun formatAddonResponseTime(durationMs: Long): String {
    val safeMs = durationMs.coerceAtLeast(1L)
    if (safeMs < 1_000L) return "$safeMs ms"

    val roundedTenths = safeMs / 100L + if (safeMs % 100L >= 50L) 1L else 0L
    val seconds = roundedTenths / 10L
    val tenths = roundedTenths % 10L
    return if (tenths == 0L) "$seconds s" else "$seconds.$tenths s"
}

// Monotonic age avoids incorrect timestamps when the device clock changes.
internal fun formatAddonLatencyAge(age: Duration): String {
    val minutes = age.inWholeMinutes.coerceAtLeast(0L)
    return when {
        minutes == 0L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        else -> "${age.inWholeHours.coerceAtLeast(1L)}h ago"
    }
}

internal fun isAddonLatencyStale(age: Duration): Boolean = age >= 5.minutes

data class AddonsUiState(
    val addons: List<ManagedAddon> = emptyList(),
)

data class AddonOverview(
    val totalAddons: Int,
    val activeAddons: Int,
    val totalCatalogs: Int,
)

internal fun List<ManagedAddon>.toOverview(): AddonOverview =
    AddonOverview(
        totalAddons = size,
        activeAddons = count { it.isActive },
        totalCatalogs = filter { it.enabled }.sumOf { it.manifest?.catalogs?.size ?: 0 },
    )

internal fun List<ManagedAddon>.enabledAddons(): List<ManagedAddon> =
    filter { it.enabled }

internal fun List<ManagedAddon>.hasPendingEnabledManifests(): Boolean =
    any { addon -> addon.enabled && addon.isRefreshing }

internal fun List<ManagedAddon>.isWaitingForFirstEnabledManifest(): Boolean {
    val enabledAddons = enabledAddons()
    return enabledAddons.isNotEmpty() &&
        enabledAddons.none { addon -> addon.manifest != null } &&
        enabledAddons.any { addon -> addon.isRefreshing }
}

internal fun List<ManagedAddon>.firstEnabledManifestError(): String? =
    asSequence()
        .filter { addon -> addon.enabled && addon.manifest == null }
        .mapNotNull { addon -> addon.errorMessage?.takeIf(String::isNotBlank) }
        .firstOrNull()

sealed interface AddAddonResult {
    data class Success(val manifest: AddonManifest) : AddAddonResult
    data class Error(val message: String) : AddAddonResult
}
