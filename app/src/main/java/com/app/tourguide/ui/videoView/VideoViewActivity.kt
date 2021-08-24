package com.app.tourguide.ui.videoView

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.app.tourguide.R
import com.app.tourguide.utils.Constants
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.fragment_video_view.*

class VideoViewActivity : AppCompatActivity() {

    var dataSourceFac: DataSource.Factory? = null

    companion object {
        var nPlayer: ExoPlayer? = null
    }

    var duration: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view)
        if (savedInstanceState != null) duration = savedInstanceState.getLong("duration")
        getBundledArguments()
        ivCancel.setOnClickListener {
            if (nPlayer != null) {
                nPlayer!!.release()
                nPlayer = null
            }
            onBackPressed()
        }
    }

    private fun getBundledArguments() {
        if (intent.getStringExtra(Constants.VIDEO_URL) != null)
            playVideo(intent.getStringExtra(Constants.VIDEO_URL)!!)
        else
            onBackPressed()
    }

    private fun playVideo(videoUrl: String) {

        if (nPlayer != null) {
            nPlayer!!.release()
            nPlayer = null
        }

        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, this!!.getString(R.string.app_name)), defaultBandwidthMeter)
        dataSourceFac = dataSourceFactory


        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(defaultBandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val contentMediaSource = buildMediaSource(Uri.parse(videoUrl))

        val mediaSources = arrayOfNulls<MediaSource>(2) //The Size must change depending on the Uris
        mediaSources[0] = contentMediaSource //uri
        val subtitleSource = SingleSampleMediaSource(Uri.parse("" +
                "https://www.iandevlin.com/html5test/webvtt/upc-video-subtitles-en.vtt"),
                dataSourceFactory, Format.createTextSampleFormat(null, MimeTypes.TEXT_VTT, Format.NO_VALUE, "en", null),
                C.TIME_UNSET)

        // mediaSources[1] = subtitleSource

        //val mediaSource = MergingMediaSource(mediaSources[0], mediaSources[1])

        nPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        (nPlayer as SimpleExoPlayer?)!!.seekTo(duration)
        (nPlayer as SimpleExoPlayer?)!!.playWhenReady = true
        playerView.player = nPlayer

        (nPlayer as SimpleExoPlayer?)!!.prepare(contentMediaSource)
        nPlayer = nPlayer


        nPlayer?.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            }

            override fun onSeekProcessed() {
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
            }

            override fun onLoadingChanged(isLoading: Boolean) {
            }

            override fun onPositionDiscontinuity(reason: Int) {
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            }

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (nPlayer != null) {
                            nPlayer!!.release()
                            nPlayer = null
                            onBackPressed()
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        pbBuffering.visibility = View.VISIBLE
                    }

                    Player.STATE_READY -> {
                        pbBuffering.visibility = View.GONE
                    }
                }

            }

        })

    }


    private fun buildMediaSource(uri: Uri): MediaSource {
        @C.ContentType val type = Util.inferContentType(uri)
        when (type) {
            /*C.TYPE_DASH:
               return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
            C.TYPE_SS:
               return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);*/
            C.TYPE_HLS -> return HlsMediaSource.Factory(dataSourceFac).createMediaSource(uri)
            C.TYPE_OTHER -> return ExtractorMediaSource.Factory(dataSourceFac).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type") as Throwable
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (nPlayer != null) {
            nPlayer!!.release()
            nPlayer = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState!!.putLong("duration", nPlayer!!.currentPosition)
    }
}
