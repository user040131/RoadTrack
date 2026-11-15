package ai.roadtrack.app

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore

/** [colorArgb]: ambient light color picked by whoever pinned this song; null falls back to the mood color. */
data class Song(val title: String, val artist: String, val colorArgb: Int? = null)

/** Hands playback to whichever music app the user has (Melon, YouTube Music, ...). We never stream audio. */
fun playSong(context: Context, song: Song): Boolean {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
        putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
        putExtra(MediaStore.EXTRA_MEDIA_TITLE, song.title)
        putExtra(MediaStore.EXTRA_MEDIA_ARTIST, song.artist)
        putExtra(SearchManager.QUERY, "${song.title} ${song.artist}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try { context.startActivity(intent); true } catch (e: ActivityNotFoundException) { false }
}
