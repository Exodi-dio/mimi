package com.exodidio.mimi.library

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reading and writing the files the listener picks.
 *
 * Everything the app saves outside itself goes through the system's own file picker, so
 * no permission over the whole of storage is ever asked for and the app can only reach
 * the one file or folder the listener chose. That is also why a folder is held as the
 * address the system gave rather than as a path on the device.
 */

/**
 * The text of a document, read.
 *
 * Null when the document could not be read at all, which is what a revoked permission or
 * a file that has been moved looks like from here.
 */
suspend fun readDocumentText(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        }
    }.getOrNull()
}

/**
 * The text of a document, written.
 *
 * The stream is opened for writing and truncating, so a file that is written twice does
 * not keep the tail of the first time.
 */
suspend fun writeDocumentText(context: Context, uri: Uri, text: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@runCatching false
            stream.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            true
        }.getOrDefault(false)
    }

/**
 * A new document of [name] inside the folder [treeUri], written with [text].
 *
 * The address of the new document is returned, or null when the folder would not take
 * it. A folder the listener chose can still be full, or on a volume that has gone away,
 * and saying so is better than pretending the file was written.
 */
suspend fun writeDocumentInTree(
    context: Context,
    treeUri: String,
    name: String,
    text: String,
    mimeType: String = "text/plain",
): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val tree = Uri.parse(treeUri)
        val parent = DocumentsContract.getTreeDocumentId(tree)
        val directory = DocumentsContract.buildDocumentUriUsingTree(tree, parent)
        val created = DocumentsContract.createDocument(
            context.contentResolver,
            directory,
            mimeType,
            name,
        ) ?: return@runCatching null
        if (writeDocumentText(context, created, text)) created else null
    }.getOrNull()
}

/**
 * The name of a document as the system calls it.
 *
 * The name of the file a listener picked is what a playlist made from it should be
 * called, so it is asked of the system rather than guessed at from the address.
 */
suspend fun documentDisplayName(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver
            .query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()
}

/**
 * The name of a folder as the system calls it.
 *
 * The address of a chosen folder ends in the document id of the folder itself, which
 * reads as the volume and the path within it. The path is what the listener recognises,
 * so that is what is shown.
 */
fun treeFolderLabel(treeUri: String): String {
    val documentId = runCatching {
        DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
    }.getOrNull() ?: return ""
    return folderLabel(documentId)
}

/**
 * The name of a folder from the document id the system gave it.
 *
 * Pure so the wording can be checked without a device: the id reads as the volume and
 * the path within it, and a folder at the top of the volume has no path to show.
 */
fun folderLabel(documentId: String): String {
    val path = documentId.substringAfter(':', "")
    return path.trim('/').ifEmpty { documentId.substringBefore(':') }
}

/**
 * A file name that a file system will accept.
 *
 * A playlist or a backup is named after what it holds, and a name can hold a slash, which
 * a file system reads as a folder. The marks that cannot be in a name are turned into
 * dashes rather than being dropped, so two playlists rarely end up with the same name.
 */
fun safeFileName(name: String): String {
    val cleaned = name.trim()
        .replace('/', '-')
        .replace('\\', '-')
        .replace(':', '-')
        .replace('*', '-')
        .replace('?', '-')
        .replace('"', '-')
        .replace('<', '-')
        .replace('>', '-')
        .replace('|', '-')
    return cleaned.ifBlank { "playlist" }
}
