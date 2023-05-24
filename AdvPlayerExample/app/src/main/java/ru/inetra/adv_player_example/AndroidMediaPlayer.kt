package ru.inetra.adv_player_example

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.View
import ru.inetra.adv.AdvPlayer

class AndroidMediaPlayer(
    private val context: Context
) : AdvPlayer.MediaPlayer {
    private var seekPosWhenPrepared = 0L
    private var player: MediaPlayer? = null
    private var surfaceHolder: SurfaceHolder? = null

    init {
        player = createMediaPlayer()
    }

    private fun createMediaPlayer(): MediaPlayer {
        val player = MediaPlayer()

        player.setOnPreparedListener { mp: MediaPlayer ->
            setState(AdvPlayer.MediaPlayer.PlayerState.LOADED)
            if (seekPosWhenPrepared != 0L) {
                seekTo(seekPosWhenPrepared)
                seekPosWhenPrepared = 0
            }
            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                setState(AdvPlayer.MediaPlayer.PlayerState.PLAYING)
            }
            mp.start()
        }

        player.setOnVideoSizeChangedListener { mp: android.media.MediaPlayer, width: Int, height: Int ->
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight

            if (getState() == AdvPlayer.MediaPlayer.PlayerState.LOADED) {
                setState(AdvPlayer.MediaPlayer.PlayerState.PLAYING)
            }

            playerListeners.forEach { it.onVideoSizeChanged(videoWidth, videoHeight, 1f) }
        }

        player.setOnCompletionListener { _ ->
            playerListeners.forEach { it.onComplete() }
            stop()
        }

        player.setOnErrorListener { mp, framework_err, impl_err ->
            playerListeners.forEach { it.onError(AdvPlayer.Error(AdvPlayer.ErrorType.UNDEFINED, "")) }
            stop()
            true
        }

        player.setScreenOnWhilePlaying(true)

        surfaceHolder?.let { player.setDisplay(it) }

        currentState = AdvPlayer.MediaPlayer.PlayerState.STOPPED

        return player
    }

    private val playerListeners = mutableListOf<AdvPlayer.MediaPlayer.Listener>()
    override fun addListener(listener: AdvPlayer.MediaPlayer.Listener) {
        playerListeners.add(listener)
    }

    override fun removeListener(listener: AdvPlayer.MediaPlayer.Listener) {
        playerListeners.remove(listener)
    }

    private var currentState = AdvPlayer.MediaPlayer.PlayerState.STOPPED
    private fun setState(state: AdvPlayer.MediaPlayer.PlayerState) {
        if (currentState != state) {
            currentState = state
            playerListeners.forEach { it.onStateChanged(state) }
        }
    }

    override fun getState(): AdvPlayer.MediaPlayer.PlayerState {
        return currentState
    }

    override fun play(uri: String, isAdvUri: Boolean, positionMs: Long) {
        seekPosWhenPrepared = positionMs

        stop()

        player = createMediaPlayer()

        setState(AdvPlayer.MediaPlayer.PlayerState.LOADING)

        try {
            player?.setDataSource(context, Uri.parse(uri))
            player?.prepareAsync()
        } catch (exc: Exception) {
            playerListeners.forEach {
                it.onError(
                    AdvPlayer.Error(
                        AdvPlayer.ErrorType.MEDIA_NOTFOUND,
                        exc.message ?: "undefined"
                    )
                )
            }
        }
    }

    override fun pause() {
        player?.pause()

        setState(AdvPlayer.MediaPlayer.PlayerState.PAUSED)
    }

    override fun seekTo(msec: Long) {
        when (getState()) {
            AdvPlayer.MediaPlayer.PlayerState.PLAYING,
            AdvPlayer.MediaPlayer.PlayerState.LOADED -> player?.seekTo(msec.toInt())

            AdvPlayer.MediaPlayer.PlayerState.PAUSED -> {
                player?.seekTo(msec.toInt())
                resume()
            }

            else -> seekPosWhenPrepared = msec
        }
    }

    override fun stop() {
        seekPosWhenPrepared = 0

        if (getState() == AdvPlayer.MediaPlayer.PlayerState.LOADING) {
            player?.release()
            player = null
        } else {
            player?.reset()
        }

        setState(AdvPlayer.MediaPlayer.PlayerState.STOPPED)
    }

    override fun resume() {
        player?.start()

        setState(AdvPlayer.MediaPlayer.PlayerState.PLAYING)
    }

    override fun destroy() {
        stop()
        player = null
    }

    override fun getCurrentPosition(): Long {
        return player?.currentPosition?.toLong() ?: 0
    }

    override fun getDuration(): Long {
        return player?.duration?.toLong() ?: 0
    }

    override fun setVolume(volume: Float) {
        player?.setVolume(volume, volume)
    }

    override fun isPlaying(): Boolean {
        return getState() == AdvPlayer.MediaPlayer.PlayerState.PLAYING
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        this.surfaceHolder = surfaceHolder
        player?.setDisplay(surfaceHolder)
    }

    override fun getSubtitleView(): View? {
        return null
    }
}