package ma.midoxshop.poster

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

/**
 * Midox is where the products live: Poster reads them, with their photos, from Midox's catalog
 * (only apps signed with the shop's key may). Nothing is ever written back.
 */
object Midox {
    const val PACKAGE = "ma.midoxshop.prices"
    private const val BASE = "content://ma.midoxshop.prices.catalog"

    fun photo(name: String): Uri = Uri.parse("$BASE/photo/$name")

    fun installed(ctx: Context) = ctx.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** The products, or null when Midox isn't there (or doesn't let Poster in). */
    fun items(ctx: Context): List<Item>? = runCatching {
        ctx.contentResolver.query(Uri.parse("$BASE/products"), null, null, null, null)?.use { c ->
            fun col(n: String) = c.getColumnIndex(n)
            val out = mutableListOf<Item>()
            while (c.moveToNext()) {
                val qi = col("qty")
                out += Item(
                    id = c.getString(col("id")),
                    name = c.getString(col("name")) ?: "",
                    price = if (c.isNull(col("price"))) null else c.getDouble(col("price")),
                    cat = c.getString(col("cat")) ?: "",
                    cond = c.getString(col("cond")) ?: "",
                    qty = if (qi < 0 || c.isNull(qi)) null else c.getInt(qi),
                    available = c.getInt(col("available")) != 0,
                    pics = (c.getString(col("pics")) ?: "").split("|").filter { it.isNotBlank() }
                )
            }
            out
        }
    }.getOrNull()

    /** Midox's old Marketplace settings (key → value); empty on a Midox from before Poster. */
    fun market(ctx: Context): Map<String, String>? = runCatching {
        ctx.contentResolver.query(Uri.parse("$BASE/market"), null, null, null, null)?.use { c ->
            val m = mutableMapOf<String, String>()
            while (c.moveToNext()) m[c.getString(0)] = c.getString(1) ?: ""
            m
        }
    }.getOrNull()?.takeIf { it.containsKey("posted") }

    fun open(ctx: Context) {
        ctx.packageManager.getLaunchIntentForPackage(PACKAGE)?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/**
 * The post's photos go into a "Midox Marketplace" album, newest first, so Facebook's photo picker
 * shows them on top. Done takes them out again. One post's photos at a time. Android 10 and up.
 */
object Album {
    private const val ALBUM = "Midox Marketplace"

    fun works() = Build.VERSION.SDK_INT >= 29

    private fun p(ctx: Context) = ctx.getSharedPreferences("poster", Context.MODE_PRIVATE)
    private fun saved(ctx: Context) = (p(ctx).getString("album", "") ?: "").lines().filter { it.isNotBlank() }
    private fun save(ctx: Context, uris: List<String>) { p(ctx).edit().putString("album", uris.joinToString("\n")).apply() }

    fun product(ctx: Context): String? = p(ctx).getString("album_pid", null)?.takeIf { saved(ctx).isNotEmpty() }

    /** Puts [item]'s photos in the album. Returns how many. */
    fun put(ctx: Context, item: Item): Int {
        if (!works()) return 0
        clear(ctx)
        val cr = ctx.contentResolver
        val now = System.currentTimeMillis()
        val uris = mutableListOf<String>()
        item.pics.withIndex().reversed().forEach { (i, name) ->
            val v = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "post-$now-${i + 1}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM)
                put(MediaStore.Images.Media.DATE_TAKEN, now - i * 1000L)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val u = runCatching { cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) }.getOrNull() ?: return@forEach
            val ok = runCatching {
                cr.openOutputStream(u)!!.use { out -> cr.openInputStream(Midox.photo(name))!!.use { it.copyTo(out) } }
                cr.update(u, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }.isSuccess
            if (ok) uris += u.toString() else runCatching { cr.delete(u, null, null) }
            save(ctx, uris)
        }
        p(ctx).edit().putString("album_pid", item.id).apply()
        return uris.size
    }

    fun clear(ctx: Context) {
        saved(ctx).forEach { s -> runCatching { ctx.contentResolver.delete(Uri.parse(s), null, null) } }
        save(ctx, emptyList())
        p(ctx).edit().remove("album_pid").apply()
    }
}
