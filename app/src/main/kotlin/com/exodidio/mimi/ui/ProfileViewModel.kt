package com.exodidio.mimi.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exodidio.mimi.MimiApplication
import com.exodidio.mimi.library.Profile
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The profile, for the avatar on the home screen and the page that changes it.
 *
 * One view model rather than each place reading the store, because the name and pictures
 * appear in two places at once: the avatar beside the home title, and the profile page it
 * opens. They are the same truth, so they are written once and observed.
 *
 * Null until the first load finishes, rather than an empty default, so the page can tell
 * "no name yet" from "not read yet" and does not overwrite what is stored with a blank
 * field that had not been filled in.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val store = (application as MimiApplication).profileStore

    private val _profile = MutableStateFlow<Profile?>(null)

    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    /**
     * Counts the pictures written, because a replaced picture keeps its file name.
     *
     * Without something that changes, the home avatar would keep drawing the picture it
     * decoded first: the profile's own flags stay true whether the avatar is the old one or
     * the new one, so nothing would tell the image to be read again.
     */
    var pictureRevision by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch { reload() }
    }

    /** The avatar file, or null when none is set. Read after [pictureRevision] changes. */
    fun avatarFile(): File? = store.avatarFile()

    /** The banner file, or null when none is set. Read after [pictureRevision] changes. */
    fun bannerFile(): File? = store.bannerFile()

    /** Keeps the name. The page saves when asked, so a discarded edit writes nothing. */
    fun saveName(name: String) {
        viewModelScope.launch {
            store.setName(name)
            reload()
        }
    }

    /** Copies [uri] in as the avatar, or removes it when null. */
    fun setAvatar(uri: Uri?) {
        viewModelScope.launch {
            store.setAvatar(uri)
            pictureRevision++
            reload()
        }
    }

    /** Copies [uri] in as the banner, or removes it when null. */
    fun setBanner(uri: Uri?) {
        viewModelScope.launch {
            store.setBanner(uri)
            pictureRevision++
            reload()
        }
    }

    /**
     * Keeps a picture the crop editor has already cut to the frame.
     *
     * The revision is bumped for the same reason the uri path bumps it: the file keeps its
     * name, so nothing else would tell the home avatar to read the new picture. The store
     * frees [bitmap] once it has been encoded, which is why it is not held in any state
     * here — a crop is several megabytes and only the file needs to outlive this call.
     */
    fun setAvatar(bitmap: Bitmap) {
        viewModelScope.launch {
            store.setAvatar(bitmap)
            pictureRevision++
            reload()
        }
    }

    /** Keeps a banner the crop editor has already cut to the frame, freeing [bitmap]. */
    fun setBanner(bitmap: Bitmap) {
        viewModelScope.launch {
            store.setBanner(bitmap)
            pictureRevision++
            reload()
        }
    }

    private suspend fun reload() {
        _profile.value = store.load()
    }
}
