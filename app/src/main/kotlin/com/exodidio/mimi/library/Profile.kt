package com.exodidio.mimi.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Who is using the app, as far as the app needs to know.
 *
 * There are no accounts here and this is not one: the name exists so a screen can greet
 * somebody, and the two flags say whether a picture was chosen. The pictures themselves
 * are files, which is why the profile carries booleans rather than bitmaps — a value that
 * can be copied around is cheaper than one that can be several megabytes.
 */
data class Profile(
    val name: String = "",
    val hasAvatar: Boolean = false,
    val hasBanner: Boolean = false,
)

/**
 * The name and pictures behind the profile, kept on the device.
 *
 * The name is one row in the `meta` table under the settings prefix, which is the same
 * place every other choice lives and needs no schema of its own. A picture cannot go
 * there: it is a few hundred kilobytes of encoded image, and the table is read whole
 * whenever a setting changes.
 *
 * Pictures are copied into the app's own storage rather than remembered by their picker
 * uri. The photo picker grants a read that lasts as long as the app is running, so a uri
 * would draw the picture today and a blank circle tomorrow; a copy keeps it for good.
 * Picking a picture also downscales it, because a twelve megapixel photograph drawn in a
 * 96dp circle is memory spent on detail nobody can see, while the file is opened every
 * time the drawer is pulled out.
 *
 * Every write is on the IO dispatcher. Nothing here touches the database or the disk on
 * the frame that draws it.
 */
class ProfileStore(private val context: Context) {

    private val metaDao by lazy { MimiDatabase.get(context).metaDao() }

    /** What is stored now, with the file checks done off the main thread. */
    suspend fun load(): Profile = withContext(Dispatchers.IO) {
        val stored = runCatching { metaDao.get(NAME_KEY) }.getOrNull().orEmpty()
        Profile(
            name = stored,
            hasAvatar = avatarFile() != null,
            hasBanner = bannerFile() != null,
        )
    }

    /** Keeps [name], which is stored trimmed because a name is not a place to keep spaces. */
    suspend fun setName(name: String) = withContext(Dispatchers.IO) {
        runCatching { metaDao.put(MetaEntry(NAME_KEY, name.trim())) }
        Unit
    }

    /** Copies the chosen picture in as the avatar, or removes the avatar for null. */
    suspend fun setAvatar(uri: Uri?) = keep(AVATAR, uri, AVATAR_EDGE)

    /** Copies the chosen picture in as the banner, or removes the banner for null. */
    suspend fun setBanner(uri: Uri?) = keep(BANNER, uri, BANNER_EDGE)

    /** The avatar file, or null when no picture has been chosen. */
    fun avatarFile(): File? = fileFor(AVATAR).takeIf(::isPicture)

    /** The banner file, or null when no picture has been chosen. */
    fun bannerFile(): File? = fileFor(BANNER).takeIf(::isPicture)

    /**
     * Decodes [uri], shrinks it to [maxEdge] and writes it as JPEG, or deletes the picture.
     *
     * The new picture is written beside the old one and renamed over it, so a decode that
     * fails halfway leaves the previous avatar in place instead of a truncated file. A uri
     * that cannot be read costs the new picture and keeps the old one, which is the kinder
     * failure: the listener sees nothing change rather than losing what was there.
     */
    private suspend fun keep(name: String, uri: Uri?, maxEdge: Int) = withContext(Dispatchers.IO) {
        val folder = folder()
        val target = File(folder, name)
        if (uri == null) {
            runCatching { target.delete() }
            return@withContext
        }
        if (!folder.isDirectory) folder.mkdirs()

        runCatching {
            val bitmap = decode(context, uri, maxEdge) ?: return@runCatching
            val scratch = File(target.parentFile, "$name.part")
            FileOutputStream(scratch).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (!scratch.renameTo(target)) {
                scratch.copyTo(target, overwrite = true)
                scratch.delete()
            }
        }
        Unit
    }

    private fun folder(): File = File(context.filesDir, FOLDER)

    private fun fileFor(name: String): File = File(folder(), name)

    /** A file with something in it, which is what a picture that finished writing looks like. */
    private fun isPicture(file: File): Boolean = file.isFile && file.length() > 0L

    private companion object {
        /**
         * Under the settings prefix so the name is read with the other choices and no
         * separate query is needed to find it.
         */
        val NAME_KEY = Settings.PREFIX + "profile_name"

        const val FOLDER = "profile"
        const val AVATAR = "avatar.jpg"
        const val BANNER = "banner.jpg"

        /** Bigger than the 96dp circle it is drawn in, so it survives a dense screen. */
        const val AVATAR_EDGE = 512

        /** Wide enough for a tablet's drawer, small enough to keep the file quick to open. */
        const val BANNER_EDGE = 1600

        const val JPEG_QUALITY = 90
    }
}

/**
 * The letter drawn on an avatar that has no picture.
 *
 * Null for a blank name so the caller can draw a neutral person glyph instead of an empty
 * circle. Uppercasing does not consult the device's locale on purpose: a name is a name,
 * and a Turkish dotless i would turn one initial into a different letter.
 */
fun profileInitial(name: String): String? =
    name.trim().firstOrNull()?.uppercaseChar()?.toString()

/**
 * The factor a picture of [longestEdge] pixels is halved by before it is decoded.
 *
 * Sampling is the only way to decode a large photograph without holding all of it, and it
 * only halves, so the answer is a power of two that lands just above [targetPx] rather
 * than exactly on it: a second small scale finishes the job afterwards. Pure, so the rule
 * can be checked without a device.
 */
internal fun sampleSizeFor(longestEdge: Int, targetPx: Int): Int {
    if (longestEdge <= 0 || targetPx <= 0) return 1
    var sampleSize = 1
    while (longestEdge / (sampleSize * 2) >= targetPx) sampleSize *= 2
    return sampleSize
}

/** Reads [uri] at a sampling that fits [maxEdge], then scales the last stretch exactly. */
private fun decode(context: Context, uri: Uri, maxEdge: Int): Bitmap? = runCatching {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), maxEdge)
    }
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return@runCatching null

    scaleDown(decoded, maxEdge)
}.getOrNull()

/**
 * The picture in [file] at no more than [maxEdge] on its longest side, for drawing.
 *
 * Kept here rather than in the screen because the drawer, the navigation bar and the
 * profile page all show the same file, and the decode rule should be one rule. Blocking,
 * so callers read it off the main thread.
 */
fun decodeProfileImage(file: File, maxEdge: Int): Bitmap? = runCatching {
    if (!file.isFile) return@runCatching null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight), maxEdge)
    }
    BitmapFactory.decodeFile(file.path, options)?.let { scaleDown(it, maxEdge) }
}.getOrNull()

/** Shrinks [bitmap] so its longest side is [maxEdge], reusing it when it already fits. */
private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val longest = maxOf(bitmap.width, bitmap.height)
    if (longest <= maxEdge || longest == 0) return bitmap

    val ratio = maxEdge.toFloat() / longest
    val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}
