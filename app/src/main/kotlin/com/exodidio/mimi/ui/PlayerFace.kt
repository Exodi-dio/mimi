package com.exodidio.mimi.ui

/**
 * Which face the player is showing.
 *
 * The artwork is the resting face; the other two are panels a listener flips to. One
 * value rather than two booleans means the faces are mutually exclusive by construction,
 * which is exactly what the skin's pager needs: it swipes between these three and the
 * state underneath does not change shape.
 */
enum class PlayerFace { ARTWORK, LYRICS, QUEUE }
