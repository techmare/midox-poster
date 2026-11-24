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
 * (only apps signed with the shop's key may). The one thing written back: a photo for a product that
 * has none ([addPhoto]), so posting fills Midox with photos too.
 */
object Midox {
    const val PACKAGE = "ma.midoxshop.prices"
    private const val BASE = "content://ma.midoxshop.prices.catalog"

    fun photo(name: String): Uri =
        if (demo) Uri.parse("file:///android_asset/demo/$name") else Uri.parse("$BASE/photo/$name")

    /** A product photo's bytes, for the gallery album. */
    fun openPhoto(ctx: Context, name: String): java.io.InputStream? =
        if (demo) runCatching { ctx.assets.open("demo/$name") }.getOrNull()
        else runCatching { ctx.contentResolver.openInputStream(photo(name)) }.getOrNull()

    /**
     * Debug builds only, for the Screen check (an emulator has no Midox): sample products with photos
     * from the debug assets. Switched on by opening Poster with the "demo" extra.
     */
    @Volatile var demo = false

    private val DEMO = listOf(
        Item("d750", "Nikon D750 + 24-120mm f/4", 9800.0, "Appareils photo", "Used", 1, true, listOf("d750-1.jpg", "d750-2.jpg")),
        Item("a7", "Sony A7 III", 12500.0, "Appareils photo", "Used", 1, true, listOf("a7-1.jpg", "a7-2.jpg")),
        Item("v1", "Godox V1 Nikon", 2300.0, "Flashs", "New", 4, true, listOf("v1-1.jpg")),
        Item("r6", "Canon EOS R6", 19500.0, "Appareils photo", "New", 2, true, listOf("r6-1.jpg", "r6-2.jpg")),
        Item("sigma", "Sigma 35mm f/1.4 Art Canon", 5200.0, "Objectifs", "Used", 1, true, emptyList()),
        Item("rs3", "DJI RS 3 Mini", 3900.0, "Stabilisateurs", "New", 3, true, listOf("rs3-1.jpg"))
    )

    fun installed(ctx: Context) = ctx.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** Why Poster can't read the products, so Today says the right fix. */
    enum class Problem { NOT_INSTALLED, REINSTALL_POSTER, UPDATE_MIDOX }

    fun problem(ctx: Context): Problem = when {
        !installed(ctx) -> Problem.NOT_INSTALLED
        // Android gives Midox's permission only to apps installed after Midox: Poster came first.
        ctx.checkSelfPermission(PERMISSION) != android.content.pm.PackageManager.PERMISSION_GRANTED -> Problem.REINSTALL_POSTER
        else -> Problem.UPDATE_MIDOX
    }
    private const val PERMISSION = "ma.midoxshop.prices.permission.READ_CATALOG"

    /** The products, or null when Midox isn't there (or doesn't let Poster in). */
    fun items(ctx: Context): List<Item>? = if (demo) DEMO else runCatching {
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
    fun market(ctx: Context): Map<String, String>? = if (demo) null else runCatching {
        ctx.contentResolver.query(Uri.parse("$BASE/market"), null, null, null, null)?.use { c ->
            val m = mutableMapOf<String, String>()
            while (c.moveToNext()) m[c.getString(0)] = c.getString(1) ?: ""
            m
        }
    }.getOrNull()?.takeIf { it.containsKey("posted") }

    /** What happened to a photo sent to Midox. */
    enum class Sent { ADDED, OLD_MIDOX, FULL, GONE, BAD_PHOTO }

    /**
     * Sends a photo taken or picked in Poster to Midox, for product [pid]: written to Midox's incoming
     * folder, then added to the product (Midox turns it upright and resizes it, like its own). Returns
     * what happened and, when added, the photo's name in Midox. Slow: call it off the main thread.
     */
    fun addPhoto(ctx: Context, pid: String, from: Uri): Pair<Sent, String?> {
        if (demo) return Sent.OLD_MIDOX to null
        val cr = ctx.contentResolver
        val bytes = runCatching { cr.openInputStream(from)!!.use { it.readBytes() } }.getOrNull()
            ?.takeIf { it.isNotEmpty() } ?: return Sent.BAD_PHOTO to null
        val file = java.util.UUID.randomUUID().toString().replace("-", "") + ".jpg"
        // A Midox from before 3.0.29 has no incoming folder: writing fails, so it needs an update.
        runCatching { cr.openOutputStream(Uri.parse("$BASE/incoming/$file"), "w")!!.use { it.write(bytes) } }
            .onFailure { return Sent.OLD_MIDOX to null }
        val answer = runCatching { cr.call(Uri.parse(BASE), "add_photo", pid, android.os.Bundle().apply { putString("file", file) }) }
            .getOrNull() ?: return Sent.OLD_MIDOX to null
        answer.getString("name")?.let { return Sent.ADDED to it }
        return when (answer.getString("error")) {
            "full" -> Sent.FULL
            "no_product" -> Sent.GONE
            else -> Sent.BAD_PHOTO
        } to null
    }

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

    /** Whose photos the album holds (or is being filled with). */
    fun product(ctx: Context): String? = p(ctx).getString("album_pid", null)

    /** Puts [item]'s photos in the album. Returns how many. */
    fun put(ctx: Context, item: Item): Int {
        if (!works()) return 0
        clear(ctx)
        p(ctx).edit().putString("album_pid", item.id).apply()
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
                cr.openOutputStream(u)!!.use { out -> Midox.openPhoto(ctx, name)!!.use { it.copyTo(out) } }
                cr.update(u, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }.isSuccess
            if (ok) uris += u.toString() else runCatching { cr.delete(u, null, null) }
            save(ctx, uris)
        }
        return uris.size
    }

    fun clear(ctx: Context) {
        saved(ctx).forEach { s -> runCatching { ctx.contentResolver.delete(Uri.parse(s), null, null) } }
        save(ctx, emptyList())
        p(ctx).edit().remove("album_pid").apply()
    }
}
