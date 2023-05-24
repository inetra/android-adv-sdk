package ru.inetra.adv_player_example

import android.content.Context
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Util
import ru.inetra.adv.AdvPlayer

internal class ExoMediaPlayer(
    private val context: Context,
    parentView: ViewGroup,
    private val progressBar: ProgressBar
) : ExoPlayer.EventListener, SimpleExoPlayer.VideoListener, AdvPlayer.MediaPlayer {
    private var player: SimpleExoPlayer?
    private var subtitleView: SubtitleView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var surfaceView: SurfaceView? = null
    private val userAgent: String = Util.getUserAgent(context, "Exo2")

    init {
        player = createPlayer()
        attachPlayer(parentView)
        player?.addListener(this)
    }

    private fun createPlayer(): SimpleExoPlayer {
        // Create a default TrackSelector
        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        // Create a default LoadControl
        val loadControl: LoadControl = DefaultLoadControl()

        // Create the player
        return ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl)
    }

    private fun buildMediaSource(videoUri: String?): MediaSource? {
        if (videoUri == null) {
            return null
        }
        val contentUri = Uri.parse(videoUri)

        // Measures bandwidth during playback. Can be null if not required.
        val defaultBandwidthMeter = DefaultBandwidthMeter()

        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(context, userAgent, defaultBandwidthMeter)

        if (contentUri.scheme?.startsWith("http") == true && contentUri.lastPathSegment != null) {

            val type = Util.inferContentType(contentUri.lastPathSegment)

            when (type) {
                C.TYPE_HLS -> return HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(contentUri)
                C.TYPE_SS -> return SsMediaSource.Factory(
                        DefaultSsChunkSource.Factory(dataSourceFactory),
                        dataSourceFactory
                    )
                    .createMediaSource(contentUri)
                C.TYPE_DASH -> return DashMediaSource.Factory(
                        DefaultDashChunkSource.Factory(
                            dataSourceFactory
                        ), dataSourceFactory
                    )
                    .createMediaSource(contentUri)
            }
        }

        return ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(contentUri)
    }

    private fun attachPlayer(parentView: ViewGroup) {
        surfaceView = SurfaceView(parentView.context)
        parentView.addView(surfaceView)
        surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceHolder = holder
                player?.setVideoSurfaceHolder(surfaceHolder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceHolder = null
                player?.setVideoSurfaceHolder(null)
            }
        })
        subtitleView = SubtitleView(parentView.context)
        player?.setTextOutput { cues ->
            subtitleView?.onCues(cues)
        }
    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
    }

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerStateChanged(
        playWhenReady: Boolean,
        playbackState: Int
    ) {
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING ->
                progressBar.visibility = View.VISIBLE
            ExoPlayer.STATE_IDLE -> {
            }
            ExoPlayer.STATE_READY -> {
                progressBar.visibility = View.GONE
                if (getState() == AdvPlayer.MediaPlayer.PlayerState.LOADING) {
                    setState(AdvPlayer.MediaPlayer.PlayerState.LOADED)
                }
                setState(if (playWhenReady) AdvPlayer.MediaPlayer.PlayerState.PLAYING else AdvPlayer.MediaPlayer.PlayerState.PAUSED)
            }
            ExoPlayer.STATE_ENDED -> {
                playerListeners.forEach { it.onComplete() }
                setState(AdvPlayer.MediaPlayer.PlayerState.STOPPED)
            }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        playerListeners.forEach {
            it.onError(
                AdvPlayer.Error(
                    AdvPlayer.ErrorType.UNDEFINED,
                    error.message ?: "unknown error"
                )
            )
        }
    }

    fun release() {
        player?.release()
    }

    fun setPlayWhenReady(enabled: Boolean) {
        player?.playWhenReady = enabled
    }

    private val playerListeners = mutableListOf<AdvPlayer.MediaPlayer.Listener>()

    override fun addListener(listener: AdvPlayer.MediaPlayer.Listener) {
        playerListeners.add(listener)
    }

    override fun removeListener(listener: AdvPlayer.MediaPlayer.Listener) {
        playerListeners.remove(listener)
    }

    override fun play(uri: String, isAdvUri: Boolean, positionMs: Long) {
        player?.prepare(buildMediaSource(uri))
        player?.playWhenReady = true
        if (positionMs > 0) {
            player?.seekTo(positionMs)
        }
        setState(AdvPlayer.MediaPlayer.PlayerState.LOADING)
    }

    override fun pause() {
        player?.playWhenReady = false
    }

    override fun seekTo(msec: Long) {
        player?.seekTo(msec)
    }

    override fun stop() {
        player?.stop()
        setState(AdvPlayer.MediaPlayer.PlayerState.STOPPED)
    }

    override fun resume() {
        player?.playWhenReady = true
    }

    override fun destroy() {
        stop()
        player?.release()
        player = null
    }

    override fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }

    override fun getDuration(): Long {
        return player?.duration ?: -1
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume
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

    override fun isPlaying(): Boolean {
        return currentState == AdvPlayer.MediaPlayer.PlayerState.PLAYING
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        player?.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun getSubtitleView(): View? {
        return subtitleView
    }

    override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
        playerListeners.forEach { it.onVideoSizeChanged(width, height, pixelWidthHeightRatio) }
    }

    override fun onRenderedFirstFrame() {
    }
}