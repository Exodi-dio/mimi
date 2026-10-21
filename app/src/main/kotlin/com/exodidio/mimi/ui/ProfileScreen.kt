package com.exodidio.mimi.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The trim of the avatar where it hangs below the banner, in dp. */
private val AVATAR_OVERHANG = 48.dp

/**
 * The name and pictures behind the avatar at the top right of the home screen, and the only
 * place they are changed.
 *
 * A page with no settings rows on it, because a profile is not one: there is a banner, a
 * picture, a name and the buttons for those three, and anything else here would be a
 * settings page wearing a picture.
 *
 * Pictures are copied the instant one is chosen. The picker hands out a read that lasts
 * only as long as the app is running, so waiting for Save would leave a picture that is
 * gone the next time the app opens; the copy is made and the rest of the page can still be
 * discarded. The back arrow therefore discards the name and nothing that was already
 * written.
 */
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stored by profileViewModel.profile.collectAsStateWithLifecycle()
    val revision = profileViewModel.pictureRevision

    val avatarFile = remember(stored?.hasAvatar, revision) { profileViewModel.avatarFile() }
    val bannerFile = remember(stored?.hasBanner, revision) { profileViewModel.bannerFile() }
    val avatar = rememberProfileImage(avatarFile, PROFILE_AVATAR_PX, revision)
    val banner = rememberProfileImage(bannerFile, PROFILE_BANNER_PX, revision)

    // Seeded once, from the first load that arrives. Keying on every change would rewrite
    // the field underneath somebody who had already started typing a new name.
    var name by rememberSaveable { mutableStateOf("") }
    var seeded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(stored) {
        val loaded = stored
        if (!seeded && loaded != null) {
            name = loaded.name
            seeded = true
        }
    }

    val picturePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) profileViewModel.setAvatar(uri) }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) profileViewModel.setBanner(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "Profile", onBack = onBack)

        Box(modifier = Modifier.fillMaxWidth()) {
            BannerArea(
                image = banner,
                onClick = {
                    bannerPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
            ProfileAvatar(
                image = avatar,
                name = name.ifBlank { stored?.name.orEmpty() },
                size = 96.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = AVATAR_OVERHANG)
                    .padding(start = 20.dp),
            )
        }

        // Room for the part of the avatar that hangs below the banner.
        Spacer(modifier = Modifier.height(AVATAR_OVERHANG + 8.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            PictureActions(
                changeLabel = "Change picture",
                canRemove = stored?.hasAvatar == true,
                onChange = {
                    picturePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { profileViewModel.setAvatar(null) },
            )

            PictureActions(
                changeLabel = "Change banner",
                canRemove = stored?.hasBanner == true,
                onChange = {
                    bannerPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { profileViewModel.setBanner(null) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    profileViewModel.saveName(name)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * The 16:9 strip at the top, rounded along its bottom edge.
 *
 * Sixteen by nine rather than a fixed height because the same picture is drawn in the
 * drawer at a different width, and an aspect ratio is what keeps the crop looking the same
 * in both places. Tapping it changes the banner, which is the gesture the picture invites.
 */
@Composable
private fun BannerArea(
    image: ImageBitmap?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "Add a banner",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A change button and, once there is something to remove, a quiet way to remove it. */
@Composable
private fun PictureActions(
    changeLabel: String,
    canRemove: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onChange,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(changeLabel)
        }
        if (canRemove) {
            TextButton(
                onClick = onRemove,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("Remove")
            }
        }
    }
}

/** The avatar edge in pixels, matching what the store writes so nothing is upscaled. */
private const val PROFILE_AVATAR_PX = 512

/** The banner edge in pixels, wide enough for a tablet and cheap enough to open. */
private const val PROFILE_BANNER_PX = 1600
