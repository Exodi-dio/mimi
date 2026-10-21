package com.exodidio.mimi.lyrics

/**
 * The objects held in a named array field of a JSON answer.
 *
 * [LyricsJson.objects] deliberately steps over nested arrays, because nothing it reads is
 * nested. One provider does nest: its answer is `{"candidates": [...]}`, so the array has
 * to be found first and then read as though it were the whole answer — which it is, to
 * everything downstream.
 *
 * A field that is absent, or a body that is not JSON at all, gives back nothing rather
 * than throwing: this reads a network reply, and a song without lyrics is the acceptable
 * outcome of a broken one.
 */
fun arrayFieldIn(raw: String, field: String): List<Map<String, String>> = runCatching {
    val named = raw.indexOf("\"$field\"")
    if (named < 0) return@runCatching emptyList()
    val open = raw.indexOf('[', named)
    if (open < 0) return@runCatching emptyList()
    LyricsJson.objects(raw.substring(open))
}.getOrDefault(emptyList())
