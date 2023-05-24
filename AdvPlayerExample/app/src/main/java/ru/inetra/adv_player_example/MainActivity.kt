package ru.inetra.adv_player_example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
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
        seekBar = seek_bar_view

        val config = AdvPlayer.Config(
            authClientId = PersonalConfig.authClientId,
            authClientSecret = PersonalConfig.authClientSecret,
            prerollPlaceIds = PersonalConfig.prerollPlaceIds
        )

        mediaPlayer = AdvPlayer(
            parentVideoLayout,
            baseContext,
            AndroidMediaPlayer(baseContext),
            config,
            object : AdvPlayer.SurfaceViewFactory {
                override fun create(): SurfaceView {
                    return SurfaceView(baseContext)
                }
            }
        )

//        mediaPlayer = AdvPlayer(
//            parentVideoLayout,
//            baseContext,
//            ExoMediaPlayer(baseContext, parentVideoLayout, seekBar),
//            config
//        )

        button_start.setOnClickListener { play(videoUrl) }
        button_stop.setOnClickListener { stop() }
        button_pause.setOnClickListener { pause() }
        button_ffwd.setOnClickListener { ffwd() }
        button_rewd.setOnClickListener { rewd() }
        timePosText = text_view_time_pos

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mediaPlayer.seekTo(progress.toLong() * 1000)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        mediaPlayer.addAdListener(object : AdvPlayer.AdListener {
            override fun onAdLoadingFinished(hasAdv: Boolean) {
                Log.i("AdvTrace:", "onAdLoadingFinished: hasAdv=$hasAdv")
            }

            override fun onAdStarted(adSystemId: Int, adSystemName: String, adPlaceId: String) {
                Log.i(
                    "AdvTrace:",
                    "onAdStarted: adSystemId=$adSystemId, adSystemName=$adSystemName, adPlaceId=$adPlaceId"
                )
                seekBar.visibility = View.GONE
            }

            override fun onAdStopped() {
                seekBar.visibility = View.VISIBLE
            }

            override fun onAdClick(adUri: String?) {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(adUri)
                )
                startActivity(browserIntent)
            }

            override fun onAdError(description: String) {
                Log.i("AdvTrace:", "onAdError: $description")
            }

            override fun onAdEvent(
                adEvent: AdvPlayer.AdEvent,
                adSystemId: Int,
                adSystemName: String,
                adPlaceId: String,
                extraInfo: String
            ) {
                Log.i(
                    "AdvTrace:",
                    "onAdEvent: adEvent=$adEvent, adSystemId=$adSystemId, adSystemName=$adSystemName, adPlaceId=$adPlaceId, $extraInfo"
                )
            }
        })

        mediaPlayer.addListener(object : AdvPlayer.Listener {
            override fun onStateChanged(state: AdvPlayer.PlayerState) {
                if (state == AdvPlayer.PlayerState.LOADED) {
                    seekBar.max = mediaPlayer.getDuration().toInt()
                    updateTimePos()
                }
            }
        })

        mediaPlayer.surfaceViewLayoutListener = object : AdvPlayer.SurfaceViewLayoutListener {
            override fun onLayout(
                surfaceView: SurfaceView,
                changed: Boolean,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int
            ) {
                // Used only if custom surface view was passed via factory
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress.toLong())
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
        val adPlace = PersonalConfig.prerollPlaceIds[0]
        mediaPlayer.play(adPlace, videoUrl, 0)
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
        if (!mediaPlayer.isPlaying())
            return

        val position = mediaPlayer.getCurrentPosition() + WIND_STEP
        if (position < mediaPlayer.getDuration())
            mediaPlayer.seekTo(position)
    }

    private fun rewd() {
        if (!mediaPlayer.isPlaying())
            return

        val position = mediaPlayer.getCurrentPosition() - WIND_STEP
        if (position > 0)
            mediaPlayer.seekTo(position)
    }

    private fun updateTimePos() {
        val curPos = mediaPlayer.getCurrentPosition()
        timePosText.text = "${curPos / 1000}/${mediaPlayer.getDuration() / 1000}"
        seekBar.progress = curPos.toInt()

        if (mediaPlayer.isPlaying()) {
            mHandler.postDelayed({ updateTimePos() }, 1000)
        }
    }
}
