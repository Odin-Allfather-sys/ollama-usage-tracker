package com.odin.ollamatracker

/**
 * Theme packs per phone brand. Everything is plain color/shape data, no deps.
 */
enum class ThemePack(val label: String) {
    AUTO("Auto"),
    PIXEL("Pixel"),
    ONEUI("Samsung OneUI"),
    NOTHING("Nothing OS"),
    PLAIN("Plain");

    companion object {
        fun fromName(s: String?): ThemePack = entries.firstOrNull { it.name == s } ?: AUTO

        /** Auto-detect from manufacturer. */
        fun detectManufacturer(manufacturer: String): ThemePack = when {
            manufacturer.contains("google", true) -> PIXEL
            manufacturer.contains("samsung", true) -> ONEUI
            manufacturer.contains("nothing", true) -> NOTHING
            else -> PLAIN
        }
    }
}

data class BrandTheme(
    val pack: ThemePack,
    val bg: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val accent: Int,
    val accentTrack: Int,
    val danger: Int,
    val cornerRadiusPx: Float,
    val titleSizeSp: Float,
    val hugeMetricSizeSp: Float,
    val font: String?,   // null = system default
    val monoNumbers: Boolean,
    val listRowStyle: ListRowStyle,
)

enum class ListRowStyle { CARD, FLAT, RULED }

object Themes {
    /** Resolve AUTO to a concrete brand theme. */
    fun resolve(pack: ThemePack, ctx: android.content.Context): BrandTheme {
        val p = if (pack == ThemePack.AUTO)
            ThemePack.detectManufacturer(android.os.Build.MANUFACTURER) else pack
        return when (p) {
            ThemePack.PIXEL -> pixel(ctx)
            ThemePack.ONEUI -> oneui(ctx)
            ThemePack.NOTHING -> nothing(ctx)
            else -> plain(ctx)
        }
    }

    private fun px(ctx: android.content.Context, dp: Float) =
        dp * ctx.resources.displayMetrics.density

    fun pixel(ctx: android.content.Context) = BrandTheme(
        pack = ThemePack.PIXEL,
        bg = 0xFF0E1113.toInt(),
        surface = 0xFF1C2023.toInt(),
        surfaceAlt = 0xFF282D31.toInt(),
        textPrimary = 0xFFE8EAED.toInt(),
        textSecondary = 0xFF9AA0A6.toInt(),
        accent = 0xFF8AB4F8.toInt(),
        accentTrack = 0xFF3C4043.toInt(),
        danger = 0xFFF28B82.toInt(),
        cornerRadiusPx = px(ctx, 28f),
        titleSizeSp = 28f,
        hugeMetricSizeSp = 64f,
        font = "sans-serif",
        monoNumbers = false,
        listRowStyle = ListRowStyle.CARD,
    )

    fun oneui(ctx: android.content.Context) = BrandTheme(
        pack = ThemePack.ONEUI,
        bg = 0xFF03080F.toInt(),
        surface = 0xFF101922.toInt(),
        surfaceAlt = 0xFF1B2733.toInt(),
        textPrimary = 0xFFEAF1F8.toInt(),
        textSecondary = 0xFF90A3B5.toInt(),
        accent = 0xFF3E9BFF.toInt(),
        accentTrack = 0xFF22344A.toInt(),
        danger = 0xFFFF6E5E.toInt(),
        cornerRadiusPx = px(ctx, 32f),   // squircle-ish big radius
        titleSizeSp = 34f,               // One UI huge header
        hugeMetricSizeSp = 60f,
        font = "sans-serif-medium",
        monoNumbers = false,
        listRowStyle = ListRowStyle.CARD,
    )

    fun nothing(ctx: android.content.Context) = BrandTheme(
        pack = ThemePack.NOTHING,
        bg = 0xFF000000.toInt(),
        surface = 0xFF0A0A0A.toInt(),
        surfaceAlt = 0xFF141414.toInt(),
        textPrimary = 0xFFFFFFFF.toInt(),
        textSecondary = 0xFF888888.toInt(),
        accent = 0xFFFFFFFF.toInt(),     // mono, red only for danger
        accentTrack = 0xFF2A2A2A.toInt(),
        danger = 0xFFFF2B2B.toInt(),
        cornerRadiusPx = px(ctx, 4f),    // brutalist, near-square
        titleSizeSp = 26f,
        hugeMetricSizeSp = 58f,
        font = "monospace",
        monoNumbers = true,
        listRowStyle = ListRowStyle.RULED,
    )

    fun plain(ctx: android.content.Context) = BrandTheme(
        pack = ThemePack.PLAIN,
        bg = 0xFF101213.toInt(),
        surface = 0xFF1A1D1F.toInt(),
        surfaceAlt = 0xFF24282B.toInt(),
        textPrimary = 0xFFE6E6E6.toInt(),
        textSecondary = 0xFF9E9E9E.toInt(),
        accent = 0xFF4CAF50.toInt(),
        accentTrack = 0xFF2E3436.toInt(),
        danger = 0xFFEF5350.toInt(),
        cornerRadiusPx = px(ctx, 16f),
        titleSizeSp = 24f,
        hugeMetricSizeSp = 56f,
        font = "sans-serif",
        monoNumbers = false,
        listRowStyle = ListRowStyle.FLAT,
    )
}