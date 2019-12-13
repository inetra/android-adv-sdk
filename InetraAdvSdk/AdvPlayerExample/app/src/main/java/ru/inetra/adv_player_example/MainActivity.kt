package ru.inetra.adv_player_example

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import ru.inetra.adv.AdvPlayer

class MainActivity : AppCompatActivity() {

    private val videoUrl = "http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8"
    private lateinit var mediaPlayer: AdvPlayer
    private lateinit var timePosText: TextView
    private lateinit var seekBar: SeekBar
    private val WIND_STEP = 10000

    private val mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = AdvPlayer(parentVideoLayout, baseContext) { MediaPlayer() }

        button_start.setOnClickListener { play(videoUrl) }
        button_stop.setOnClickListener { stop() }
        button_pause.setOnClickListener { pause() }
        button_ffwd.setOnClickListener{ ffwd() }
        button_rewd.setOnClickListener{ rewd() }
        timePosText = text_view_time_pos
        seekBar = seek_bar_view

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mediaPlayer.seekTo(progress * 1000)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        mediaPlayer.addAdListener(object : AdvPlayer.AdListener {
            override fun onAdStarted() {
                seekBar.visibility = View.GONE
            }

            override fun onAdStopped() {
                seekBar.visibility = View.VISIBLE
            }

        })

        mediaPlayer.addListener(object : AdvPlayer.Listener {
            override fun onComplete() {
            }

            override fun onEndBuffering() {
            }

            override fun onError(error: AdvPlayer.Error) {
            }

            override fun onQualityChanged(bitrate: Int) {
            }

            override fun onStartBuffering() {
            }

            override fun onStateChanged(state: AdvPlayer.PlayerState) {
                if (state == AdvPlayer.PlayerState.LOADED) {
                    seekBar.max = mediaPlayer.getDuration()
                    updateTimePos()
                }
            }

            override fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
            }
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        mediaPlayer.adsEnabled = true
    }

    private fun play(videoUrl: String) {
        mediaPlayer.play(videoUrl, 0)
    }

    private fun stop() {
        mediaPlayer.stop()
    }

    private fun pause() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.pause()
        else
            mediaPlayer.resume()
    }

    private fun ffwd() {
        if(!mediaPlayer.isPlaying())
            return

        val position = mediaPlayer.getCurrentPosition() + WIND_STEP
        if (position < mediaPlayer.getDuration())
            mediaPlayer.seekTo(position)
    }

    private fun rewd() {
        if(!mediaPlayer.isPlaying())
            return

        val position = mediaPlayer.getCurrentPosition() - WIND_STEP
        if (position > 0)
            mediaPlayer.seekTo(position)
    }

    private fun updateTimePos() {
        val curPos = mediaPlayer.getCurrentPosition()
        timePosText.text = "${curPos / 1000}/${mediaPlayer.getDuration() / 1000}"
        seekBar.progress = curPos

        if (mediaPlayer.isPlaying()) {
            mHandler.postDelayed({ updateTimePos() }, 1000)
        }
    }
}
