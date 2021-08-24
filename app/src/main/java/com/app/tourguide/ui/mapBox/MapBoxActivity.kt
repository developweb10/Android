package com.app.tourguide.ui.mapBox

import Preferences
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.*
import android.graphics.Typeface.DEFAULT
import android.graphics.Typeface.create
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.app.tourguide.R
import com.app.tourguide.activity.MainActivity
import com.app.tourguide.data.NextVideoData
import com.app.tourguide.database.DatabaseClient
import com.app.tourguide.database.entity.PackageSpots
import com.app.tourguide.listeners.LocationCallBack
import com.app.tourguide.listeners.locationListener
import com.app.tourguide.listeners.onItemClickedListener
import com.app.tourguide.receiver.GpsReceiver
import com.app.tourguide.ui.mapBox.receiver.ConnectivityReceiver
import com.app.tourguide.ui.mapBox.response.*
import com.app.tourguide.utils.Constants
import com.app.tourguide.utils.saveValue
import com.app.tourguide.utils.saveValueWithApply
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Util.getUserAgent
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.directions.DirectionsCriteria
import com.mapbox.directions.MapboxDirections
import com.mapbox.directions.service.models.DirectionsResponse
import com.mapbox.directions.service.models.DirectionsRoute
import com.mapbox.directions.service.models.Waypoint
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_map_box.*
import kotlinx.android.synthetic.main.content_play_audio.*
import kotlinx.android.synthetic.main.navigation_view.*
import kotlinx.android.synthetic.main.play_tour_video.*
import org.json.JSONObject
import retrofit.Callback
import retrofit.Response
import retrofit.Retrofit
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.view.KeyEvent.KEYCODE_BACK as KEYCODE_BACK1

class MapBoxActivity : AppCompatActivity(), PermissionsListener, onItemClickedListener, locationListener, ConnectivityReceiver.ConnectivityReceiverListener {


    // UI elements
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var progressBar: ProgressBar? = null
    private var downloadButton: Button? = null
    private var listButton: Button? = null

    private var isEndNotified: Boolean = false
    private var regionSelected: Int = 0

    // Offline objects
    private var offlineManager: OfflineManager? = null
    private var offlineRegion: OfflineRegion? = null
    private var permissionsManager: PermissionsManager? = null

    private var rvSpotList: RecyclerView? = null
    private var spotListAdapter: SpotListAdapter? = null
    private var mViewModel: MapBoxViewModel? = null

    internal var LOG_TAG = "DIRECTIO"
    private var currentRoute: DirectionsRoute? = null

    private var mRegionToDownload: MapData? = null
    private var mRegionRegionSpot: List<DataItem>? = null
    private var mRegionRoute: Route? = null
    private var mRegionManualRoute: ManualItem? = null

    private val gson = Gson()
    private var packageSpotsListStr: String = ""

    private var dataSourceFac: com.google.android.exoplayer2.upstream.DataSource.Factory? = null
    private lateinit var mRegionPackageId: String
    private var mPackageTokenId: String = ""
    private var mComingFrom: String = ""
    private var mDoubleTourStatus: Boolean = false

    private var markerViewManager: MarkerViewManager? = null

    private var MAP_NORMAL: String = "normal"
    private var MAP_SATELLITE: String = "satellite"
    private lateinit var locationEngine: LocationEngine
    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 10000L

    //val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    private val callback = LocationListeningCallback(this)

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var fragment: Fragment
    private var downloadListPosition = 0
    private var downloadVttListPosition = 0

    private lateinit var mTourTimeStatus: String
    private lateinit var points: ArrayList<LatLng>
    private lateinit var dialogMsg: String
    private var dialogMsgVtt: String = ""

    private lateinit var mGlobalDialog: Dialog

    /**
     * mTourTokenStatus denotes tour active or inactive
     * true token is active
     * false toke inactive
     */
    private var mTourTokenStatus: String = "active"
    private lateinit var mTourDisTravelledByUser: String
    private val mVideoWatchedList = mutableListOf<String>()
    private var customExceptionHandlerAttached: Boolean = false

    private var STATUS_ACTIVATION = false
    private var STATUS_RETURN = false
    private var STATUS_INACTIVATION = false

    private var TYPE_ACTIVATION = "activation"
    private var TYPE_RETURN = "return"
    private var TYPE_INACTIVATION = "inactivation"
    private var TYPE_EXPIRE = "expire"
    private var mTourEndTime: Long = 0
    private var destination: String = ""
    private lateinit var ivToggleMenu: ImageView
    private lateinit var ivToggleClose: ImageView

    private var nextVideoPlay = NextVideoData(-1, false)


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_key))
        //hideStatusAndNavigationBar()
        setContentView(R.layout.activity_map_box)
        mViewModel = ViewModelProviders.of(this).get(MapBoxViewModel::class.java)
        mapView = findViewById(R.id.mapView)
        rvSpotList = findViewById(R.id.rvSpotList)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        ivToggleMenu = findViewById(R.id.iv_toggle_menu)
        ivToggleClose = findViewById(R.id.iv_toggle_close)
        callback.onLocationChangeListener(this)
        attachObserver()
        clickListeners()
        getBundleArguments()
        navigationDrawer()
        attachLocationOnOffListener()
        attachConnectivityListener()
        syncMap()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //if (hasFocus) hideSystemUI()
        hideSystemUI()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.app.tourguide.application.Application.localManger.setLocale(newBase))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }


    private fun attachConnectivityListener() {
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        showNetworkMessage(isConnected)
    }


    private fun showNetworkMessage(isConnected: Boolean) {
        if (!isConnected) {
            ivToggleMenu.visibility = View.INVISIBLE
        } else {
            ivToggleMenu.visibility = View.VISIBLE
        }
    }


    private fun syncMap() {
        mapView!!.getMapAsync { mapboxMap ->
            map = mapboxMap
            mapboxMap.setStyle(Style.SATELLITE) { style ->
                // Assign progressBar for later use
                progressBar = findViewById(R.id.progress_bar)

                // Set up the offlineManager
                offlineManager = OfflineManager.getInstance(this@MapBoxActivity)

                markerViewManager = MarkerViewManager(mapView, mapboxMap)
                mapboxMap.uiSettings.isCompassEnabled = true
                mapboxMap.uiSettings.setCompassMargins(100, 0, 5, 0)
                enableLocationComponent(style)
                ivMapStyle.tag = MAP_SATELLITE
                if (isLocationEnabled(this@MapBoxActivity)) {
                    if (isNetworkAvailable()) {
                        mViewModel?.getPackagesData(mRegionPackageId, deviceToken())
                    } else {
                        RetrievePackageSpots(baseContext, mRegionPackageId, false).execute()
                    }
                } else {
                    showLocationDialog()
                }

                downloadButton = this.findViewById(R.id.download_button)
                downloadButton!!.setOnClickListener { view -> downloadRegionDialog() }

                // List offline regions
                listButton = findViewById(R.id.list_button)
                listButton!!.setOnClickListener { view -> downloadedRegionList() }

            }
        }
    }


    //function to start tour timer
    //45 min=2700000
    private fun startTripTimeTracker(tourTime: Int): CountDownTimer {
        return object : CountDownTimer(tourTime.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                //trip limit end
                mTourTokenStatus = "inactive"
            }
        }
    }

    private var lastSpotLat: Float = 0.0f
    private var lastSpotLng: Float = 0.0f

    //Function called whenever device location changed
    override fun onLocationChanged(lat: Double, lng: Double) {
        var insidePlayStatus = false
        if (mRegionToDownload != null) {
            if (mRegionToDownload?.boothLat != "" && mRegionToDownload?.boothLong != "") {
                val distance = meterDistanceBtBoothAndUser(mRegionToDownload?.boothLat!!.toFloat(), mRegionToDownload?.boothLong!!.toFloat(), lat.toFloat(), lng.toFloat())

                if (mDoubleTourStatus) {
                    //DOUBLE TOUR PREVENTION ON
                    if (mRegionToDownload?.maxDistance != "" && mRegionToDownload?.minDistance != "") {
                        //token activation: the tablet was more than 100m from the ticket booth
                        if (!STATUS_ACTIVATION && distance > mRegionToDownload?.maxDistance!!.toDouble()) {
                            mViewModel?.updateTourTokenTime(getUpdateTokenTimeParams(TYPE_ACTIVATION))
                        }
                        //token return : this is the time that the distance to the ticket booth is less then 50m
                        if (!STATUS_RETURN && STATUS_ACTIVATION && distance < mRegionToDownload?.minDistance!!.toDouble()) {
                            mViewModel?.updateTourTokenTime(getUpdateTokenTimeParams(TYPE_RETURN))
                        }

                        //token inactive if cross tour time limit and distance greater than max distance
                        if (mTourTokenStatus == "inactive" && (distance > mRegionToDownload?.maxDistance!!.toFloat() || distance > mRegionToDownload?.minDistance!!.toFloat())
                                && !STATUS_INACTIVATION) {
                            mViewModel?.updateTourTokenTime(getUpdateTokenTimeParams(TYPE_INACTIVATION))
                        }
                    }
                    //token inactive if cross tour time limit and distance greater than max distance
                    /*if (mTourTokenStatus == "inactive" && distance > mRegionToDownload?.maxDistance!!.toFloat() && !STATUS_INACTIVATION) {
                        //takeUserToHomeScreen()
                        mViewModel?.updateTourTokenTime(getUpdateTokenTimeParams(TYPE_INACTIVATION))
                    }
                    if (mTourTokenStatus == "inactive" && distance > mRegionToDownload?.minDistance!!.toFloat() && !STATUS_INACTIVATION) {
                        //If the user tablet location becomes more then 50 meters away from the ticket booth with an inactivated token, then the tour
                        //will automatically finish and the app will return to the home screen. To see the tour again, a new token should be used.
                        //takeUserToHomeScreen()
                        mViewModel?.updateTourTokenTime(getUpdateTokenTimeParams(TYPE_INACTIVATION))
                    }*/

                    if (mTourTokenStatus == "inactive" && distance < mRegionToDownload?.minDistance!!.toFloat()) {
                        //If the user is still inside the 50m range from the ticket booth and the tour is still running inside the app,
                        // the user can still use the tour normally, including watching all videos.
                    }
                }

                //VIDEO WILL BE PLAY AUTOMATICALLY IF USER COMES INSIDE 20 METER RANGE OF SPOTS
                mRegionRegionSpot!!.forEachIndexed { index, location ->
                    if (30 > meterDistanceBtBoothAndUser(location.latitude!!.toFloat(), location.longitude!!.toFloat(), lat.toFloat(), lng.toFloat())) {
                        if (mVideoWatchedList.size > 0) {
                            if (!mVideoWatchedList.contains(location.id.toString())) {
                                insidePlayStatus = true
                                //showMessage("You have reached to spot number  ${mRegionRegionSpot!![index].spotNumber}")
                                //mVideoWatchedList.add(location.id.toString())
                                lastSpotLat = location.latitude.toFloat()
                                lastSpotLng = location.longitude.toFloat()
                                playVideoBasedOnUserLocation(index)
                            }
                        } else {
                            insidePlayStatus = true
                            //showMessage("You have reached to spot number  ${mRegionRegionSpot!![index].spotNumber}")
                            //mVideoWatchedList.add(location.id.toString())
                            lastSpotLat = location.latitude.toFloat()
                            lastSpotLng = location.longitude.toFloat()
                            playVideoBasedOnUserLocation(index)
                        }
                    }
                }
            } else {
                showMessage("minimum and maximum distance of tour not found")
            }
            //If user away from the last visited spot by 30 meter then no need to play video aftr close
            if (lastSpotLat != 0.0f && lastSpotLng != 0.0f && !insidePlayStatus) {
                if (30 < meterDistanceBtBoothAndUser(lastSpotLat, lastSpotLng, lat.toFloat(), lng.toFloat())) {
                    Log.d("ttttt", "outside of 30 metter")
                    nextVideoPlay = NextVideoData(-1, false)
                }
            }
        }
    }

    private fun playVideoBasedOnUserLocation(index: Int) {
        Log.d("ttttt", "playVideoBasedOnUserLocation $index")
        val uri: Uri? = getFileUri(index)
        val name: String = uri?.lastPathSegment as String
        val extension = name.substring(name.lastIndexOf(".") + 1)
        val data = mRegionRegionSpot?.get(index)

        if (this::mGlobalDialog.isInitialized) {
            if (mGlobalDialog.isShowing) {
                //Tour video is playing then hold the reference of next video to be play after current finished
                if (nextVideoPlay.index != index) {
                    Log.d("ttttttt", "next video play")
                    nextVideoPlay = NextVideoData(index, false)
                }
                Log.d("ttttt", "Plyyy ${nextVideoPlay.index} and ${index}")
                return  //return from here as we don't need to play this video as currently video is playing
            }
        }


        //if video to be play has same index then return
        if (nextVideoPlay.index == index && nextVideoPlay.isPlayed) {
            return
        }

        Log.d("tttttt", "adsfsadfassdfsdfdsfasfsafsdafs")
        nextVideoPlay = NextVideoData(index, true)
        //add video to watch list so that it won't play again
        mVideoWatchedList.add(data?.id.toString())
        if (data?.fileType.equals("audio")) {
            playAudio(data, uri.toString())
        } else if (data?.fileType.equals("youtube")) {
            if (data?.youtubeId != "") {
                if (data?.youtubeId!!.contains("youtube")) {
                    playYoutubeVideo(data.youtubeId)
                } else {
                    playVideoWithoutSubtitle(data.youtubeId)
                }
            } else showMessage(getString(R.string.txt_videoid_missing))
        } else if (extension == "mp4") {
            //PLAY VIDEO WITH VTT FILE
            if (data?.fileType.equals("vtt") && data?.spotVideoVtt != "") {
                playVideoWithSubtitle(uri.toString(), data)
            }
            //PLAY VIDEO WITHOUT VTT
            if (data?.fileType.equals("video") && data?.spotVideoVtt == "") {
                playVideoWithoutSubtitle(uri.toString())
            }
        } else {
            Toast.makeText(this@MapBoxActivity, "Server Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUpdateTokenTimeParams(type: String): HashMap<String, String> {
        val data = HashMap<String, String>()
        data["id"] = mPackageTokenId
        data["device_id"] = deviceToken()
        data["package_id"] = mRegionPackageId
        data["type"] = type
        return data
    }

    //Function to return distance of user from booth
    private fun meterDistanceBtBoothAndUser(lat_booth: Float, lng_booth: Float, lat_user: Float, lng_user: Float): Double {
        val pk = (180f / Math.PI).toFloat()
        val a1 = lat_booth / pk
        val a2 = lng_booth / pk
        val b1 = lat_user / pk
        val b2 = lng_user / pk

        val t1 = Math.cos(a1.toDouble()) * Math.cos(a2.toDouble()) * Math.cos(b1.toDouble()) * Math.cos(b2.toDouble())
        val t2 = Math.cos(a1.toDouble()) * Math.sin(a2.toDouble()) * Math.cos(b1.toDouble()) * Math.sin(b2.toDouble())
        val t3 = Math.sin(a1.toDouble()) * Math.sin(b1.toDouble())
        val tt = Math.acos(t1 + t2 + t3)

        return 6366000 * tt
    }

    private fun takeUserToHomeScreen(clearStatus: Boolean) {
        val timerData = TourTimePojo(mRegionPackageId, mTourEndTime, mPackageTokenId, STATUS_ACTIVATION, STATUS_RETURN, STATUS_INACTIVATION, mDoubleTourStatus)
        if (mDoubleTourStatus) {
            Preferences.prefs?.saveValue(Constants.END_TIME, gson.toJson(timerData))
        } else {
            if (clearStatus) {
                Preferences.prefs?.saveValue(Constants.END_TIME, "")
            } else {
                Preferences.prefs?.saveValue(Constants.END_TIME, gson.toJson(timerData))
            }
        }
        startActivity(Intent(this@MapBoxActivity, MainActivity::class.java).apply {
            putExtra("DESTINATION", destination)
        })

        finish()
    }


    private lateinit var mGpsReceiver: GpsReceiver

    /**
     *Function to attached location listener
     */
    private fun attachLocationOnOffListener() {
        // try {
        mGpsReceiver = GpsReceiver(object : LocationCallBack {
            override fun onLocationTriggered() {
                if (isLocationEnabled(this@MapBoxActivity)) {
                    //location enable
                    if (isNetworkAvailable()) {
                        mViewModel?.getPackagesData(mRegionPackageId, deviceToken())
                    } else {
                        RetrievePackageSpots(baseContext, mRegionPackageId, false).execute()
                    }
                } else {
                    //location disable
                    takeUserToHomeScreen(true)
                }
            }
        })
        registerReceiver(mGpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    private fun showLocationDialog() {
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
            builder.setCancelable(false)
            builder.setMessage(getString(R.string.txt_enable_location))
            builder.apply {
                setPositiveButton("Ok"
                ) { dialog, id ->
                    if (!isLocationEnabled(this@MapBoxActivity)) {
                        showLocationDialog()
                    }
                }
            }
            builder.create()
        }

        alertDialog!!.show()
    }

    fun isLocationEnabled(context: Context): Boolean {
        var locationMode = 0
        val locationProviders: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            locationMode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        } else {
            locationProviders = Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_MODE)
            return !TextUtils.isEmpty(locationProviders)
        }
    }

    /**
     * Function to handle navigation drawer clicks
     */
    private fun navigationDrawer() {
        design_navigation_view2.bringToFront()

        tv_language.setOnClickListener {
            mDrawerLayout.closeDrawer(GravityCompat.START)
            destination = "language"
            takeUserToHomeScreen(false)
        }

        tv_tour_list.setOnClickListener {
            destination = "tourlist"
            mDrawerLayout.closeDrawer(GravityCompat.START)
            takeUserToHomeScreen(false)
        }

        tv_end_tour.setOnClickListener {
            mDrawerLayout.closeDrawer(GravityCompat.START)
            if (mDoubleTourStatus) {
                mViewModel?.updateTourTokenTime(getUpdateTokenTimeParams(TYPE_EXPIRE))
            } else {
                destination = "exit"
                Preferences.prefs!!.saveValue(Constants.END_TIME, "")
                startActivity(Intent(this@MapBoxActivity, MainActivity::class.java))
                finish()
            }
        }

        mDrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(p0: Int) {
            }

            override fun onDrawerSlide(p0: View, p1: Float) {
            }

            override fun onDrawerClosed(p0: View) {
                ivToggleMenu.visibility = View.VISIBLE
                ivToggleClose.visibility = View.GONE
            }

            override fun onDrawerOpened(p0: View) {
                DrawableCompat.setTint(iv_closedrawer.drawable, ContextCompat.getColor(this@MapBoxActivity, R.color.black))
                ivToggleMenu.visibility = View.GONE
                ivToggleClose.visibility = View.VISIBLE
            }
        })

        ivToggleMenu.setOnClickListener {
            mDrawerLayout.bringToFront()
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START)
                ivToggleMenu.visibility = View.VISIBLE
                ivToggleClose.visibility = View.GONE
            } else {
                DrawableCompat.setTint(iv_closedrawer.drawable, ContextCompat.getColor(this@MapBoxActivity, R.color.black))
                ivToggleMenu.visibility = View.GONE
                ivToggleClose.visibility = View.VISIBLE
                mDrawerLayout.openDrawer(GravityCompat.START)
            }
        }


        ivToggleClose.setOnClickListener {
            mDrawerLayout.closeDrawer(GravityCompat.START)
            ivToggleMenu.visibility = View.VISIBLE
            ivToggleClose.visibility = View.GONE
        }

        iv_closedrawer.setOnClickListener {
            mDrawerLayout.closeDrawer(GravityCompat.START)
            ivToggleMenu.visibility = View.VISIBLE
        }

        ivToggleMenu.visibility = View.VISIBLE
    }


    @SuppressLint("MissingPermission")
    private fun doLocationRequest() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .setDisplacement(10F)
                .build()
        locationEngine.requestLocationUpdates(request, callback, mainLooper)
        locationEngine.getLastLocation(callback)
    }


    private class LocationListeningCallback internal constructor(activity: MapBoxActivity) : LocationEngineCallback<LocationEngineResult> {
        private lateinit var listener: locationListener
        fun onLocationChangeListener(listener: locationListener) {
            this.listener = listener
        }


        override fun onSuccess(result: LocationEngineResult?) {
            // The LocationEngineCallback interface's method which fires when the device's location has changed.
            if (result?.lastLocation != null) {
                result.lastLocation
                this.listener.onLocationChanged(result.lastLocation?.latitude!!, result.lastLocation?.longitude!!)
            }
        }

        override fun onFailure(exception: Exception) {
            // The LocationEngineCallback interface's method which fires when the device's location can not be captured
        }

    }

    @SuppressLint("MissingPermission")
    private fun clickListeners() {
        ivMapStyle.setOnClickListener {
            if (ivMapStyle.tag == MAP_NORMAL) {
                map!!.setStyle(Style.SATELLITE) { style ->
                    ivMapStyle.tag = MAP_SATELLITE
                    ivMapStyle.setBackgroundResource(R.drawable.ic_satellite)
                }
            } else if (ivMapStyle.tag == MAP_SATELLITE) {
                map!!.setStyle(Style.LIGHT) { style ->
                    ivMapStyle.tag = MAP_NORMAL
                    ivMapStyle.setBackgroundResource(R.drawable.ic_map)
                }
            }
        }

        ivNavigate.setOnClickListener {
            if (map!!.locationComponent.lastKnownLocation != null)
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(map!!.locationComponent.lastKnownLocation?.latitude!!, map!!.locationComponent.lastKnownLocation?.longitude!!), 13.0))
        }
    }

    private fun getBundleArguments() {
        mRegionPackageId = intent.getStringExtra("packageId")
        if (intent.hasExtra("packageTokenId"))
            mPackageTokenId = intent.getStringExtra("packageTokenId")
        if (intent.getStringExtra("comingFrom") != null)
            mComingFrom = intent.getStringExtra("comingFrom")
        mDoubleTourStatus = intent.getBooleanExtra("doubleTourStatus", false)
    }

    private fun attachObserver() {
        mViewModel?.response?.observe(this, { it ->
            it.let {
                if (it!!.status == 1) {
                    mRegionRegionSpot = it.data as List<DataItem>
                    mRegionToDownload = it.mapData
                    mRegionRoute = it.route
                    mRegionManualRoute = it.manual!![0]
                    packageSpotsListStr = gson.toJson(it)
                    val now = System.currentTimeMillis()
                    mTourEndTime = now + TimeUnit.MINUTES.toMillis(mRegionToDownload?.tourTime!!.toLong())
                    downloadTourPackageRegion(it.mapData?.packageId, it.mapData?.regionName)
                } else {
                    showMessage(it.message!!)
                }
            }
        })

        mViewModel?.tokenResponse?.observe(this, { it ->
            it.let {
                if (it!!.status == 1) {
                    when (it.type) {
                        TYPE_ACTIVATION -> {
                            STATUS_ACTIVATION = true
                            if (mRegionToDownload?.tourTime!!.isNotEmpty()) {
                                Preferences.initPreferences(this)
                                val gson = Gson()
                                if (mComingFrom.equals("available_location_fragment", true)) {
                                    val timerJsonData = Preferences.prefs!!.getString(Constants.END_TIME, "")
                                    //retrieve tour timer data stored in the database
                                    val timerPojo: TourTimePojo = gson.fromJson(timerJsonData, TourTimePojo::class.java)
                                    STATUS_ACTIVATION = timerPojo.statusActivation
                                    STATUS_RETURN = timerPojo.statusReturn
                                    STATUS_INACTIVATION = timerPojo.statusInactivation
                                    //resume the tour timer
                                    startTripTimeTracker((intent.getLongExtra("tourTimeLeft", 0) * 60000).toInt()).start()
                                } else {
                                    startTripTimeTracker(mRegionToDownload?.tourTime!!.toInt() * 60000).start()
                                }
                            } else showMessage("Total tour time not available")
                        }

                        TYPE_RETURN -> {
                            STATUS_RETURN = true
                        }

                        TYPE_INACTIVATION -> {
                            STATUS_INACTIVATION = true
                            takeUserToHomeScreen(true)
                        }

                        TYPE_EXPIRE -> {
                            destination = "exit"
                            Preferences.prefs!!.saveValue(Constants.END_TIME, "")
                            startActivity(Intent(this@MapBoxActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        })

        mViewModel?.apiError?.observe(this, {
            it?.let {
                showMessage(it)
                //showSnackBar(it)
            }
        })

        mViewModel?.isLoading?.observe(this, {
            it?.let {
                //showMessage(it)
                //showLoading(it)
            }
        })

        mViewModel?.onFailure?.observe(this, {
            it?.let {
                //showMessage(it)
                //showSnackBar(ApiFailureTypes().getFailureMessage(it))
            }
        })
    }


    fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        if (netInfo != null && netInfo.isConnectedOrConnecting) {
            return true
        }
        return false
    }


    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            val locationComponent = map!!.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(this, loadedMapStyle)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            //locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }


    // Override Activity lifecycle methods
    public override fun onResume() {
        super.onResume()
        mapView!!.onResume()
        doLocationRequest()
        ConnectivityReceiver.connectivityReceiverListener = this
        hideSystemUI()
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }


    override fun onStop() {
        super.onStop()
        locationEngine.removeLocationUpdates(callback)
        mapView!!.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }


    private fun pausePlayer() {
        if (nPlayer != null) {
            nPlayer!!.playWhenReady = false
            nPlayer!!.playWhenReady
        }
    }

    public override fun onPause() {
        super.onPause()
        mapView!!.onPause()
        pausePlayer()
        if (this::mGlobalDialog.isInitialized)
            mGlobalDialog.dismiss()

        if (mDoubleTourStatus) {
            if (destination != "exit") {
                val timerData = TourTimePojo(mRegionPackageId, mTourEndTime, mPackageTokenId, STATUS_ACTIVATION, STATUS_RETURN, STATUS_INACTIVATION, mDoubleTourStatus)
                Preferences.prefs?.saveValueWithApply(Constants.END_TIME, gson.toJson(timerData))
            }
        } else {
            if (destination != "exit") {
                val timerData = TourTimePojo(mRegionPackageId, mTourEndTime, mPackageTokenId, STATUS_ACTIVATION, STATUS_RETURN, STATUS_INACTIVATION, mDoubleTourStatus)
                Preferences.prefs?.saveValueWithApply(Constants.END_TIME, gson.toJson(timerData))
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        if (markerViewManager != null) {
            markerViewManager?.onDestroy()
        }
        mapView!!.onDestroy()
        unregisterReceiver(mGpsReceiver)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    fun deviceToken(): String {
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun downloadRegionDialog() {
        // Set up download interaction. Display a dialog
        // when the user clicks download button and require
        // a user-provided region name
        val builder = AlertDialog.Builder(this@MapBoxActivity)

        val regionNameEdit = EditText(this@MapBoxActivity)
        regionNameEdit.hint = getString(R.string.set_region_name_hint)

        // Build the dialog box
        builder.setTitle(getString(R.string.dialog_title))
                .setView(regionNameEdit)
                .setMessage(getString(R.string.dialog_message))
                .setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, which ->
                    val regionName = regionNameEdit.text.toString()
                    // Require a region name to begin the download.
                    // If the user-provided string is empty, display
                    // a toast message and do not begin download.
                    if (regionName.length == 0) {
                        // Toast.makeText(this@MapBoxActivity, getString(R.string.dialog_toast), Toast.LENGTH_SHORT).show()
                    } else {
                        // Begin download process
                        //downloadRegion(regionName)
                    }
                }
                .setNegativeButton(getString(R.string.dialog_negative_button)) { dialog, which -> dialog.cancel() }

        //Display the dialog
        builder.show()
    }

    private fun downloadRegion(packageId: String?, pckRegionName: String?) {
        // Define offline region parameters, including bounds,
        // min/max zoom, and metadata

        // Start the progressBar
        startProgress()

        // Create offline definition using the current
        // style and boundaries of visible map area
        val styleUrl = map!!.style!!.url

        //LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;

        val bounds = LatLngBounds.Builder()
                .include(LatLng(mRegionToDownload?.minLatitude!!.toDouble(), mRegionToDownload?.minLongitude!!.toDouble())) // Northeast
                .include(LatLng(mRegionToDownload?.maxLatitude!!.toDouble(), mRegionToDownload?.maxLongitude!!.toDouble())) // Southwest
                /*.include(LatLng(30.7168, 76.7474)) // Northeast
                .include(LatLng(30.7307, 76.7785)) // Southwest*/
                .build()

        val minZoom = mRegionToDownload?.minZoom!!.toDouble()
        val maxZoom = mRegionToDownload?.maxZoom!!.toDouble()
        /*val minZoom = map!!.getCameraPosition().zoom
        val maxZoom = map!!.getMaxZoomLevel()*/
        val pixelRatio = this.resources.displayMetrics.density

        val definition = OfflineTilePyramidRegionDefinition(styleUrl, bounds, minZoom, maxZoom, pixelRatio)

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()
        val metadata: ByteArray?
        // try {
        val jsonObject = JSONObject()
        jsonObject.put(JSON_FIELD_REGION_NAME, pckRegionName)
        jsonObject.put(JSON_FIELD_REGION_ID, packageId)
        val json = jsonObject.toString()
        metadata = json.toByteArray(charset(JSON_CHARSET))
        /*} catch (exception: Exception) {
            Timber.e("Failed to encode metadata: %s", exception.message)
            metadata = null
        }*/

        // Create the offline region and launch the download
        offlineManager!!.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                Timber.d("Offline region created: %s", packageId)
                this@MapBoxActivity.offlineRegion = offlineRegion
                launchDownload()
            }

            override fun onError(error: String) {
                Timber.e("Error: %s", error)
            }
        })
    }

    private fun launchDownload() {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion!!.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                // Compute a percentage
                val percentage = if (status.requiredResourceCount >= 0)
                    100.0 * status.completedResourceCount / status.requiredResourceCount
                else
                    0.0

                if (status.isComplete) {
                    // Download complete
                    endProgress(getString(R.string.end_progress_success))
                    //recall the method to load the downloaded region on map
                    downloadTourPackageRegion(mRegionToDownload?.packageId, mRegionToDownload?.regionName)
                    //downloadTourPackageRegion(it.mapData?.packageId, it.mapData?.regionName)
                    return
                } else if (status.isRequiredResourceCountPrecise) {
                    // Switch to determinate state
                    setPercentage(Math.round(percentage).toInt())
                }

                // Log what is being currently downloaded
                Timber.d("%s/%s resources; %s bytes downloaded.",
                        status.completedResourceCount.toString(),
                        status.requiredResourceCount.toString(),
                        status.completedResourceSize.toString())
            }

            override fun onError(error: OfflineRegionError) {
                Timber.e("onError reason: %s", error.reason)
                Timber.e("onError message: %s", error.message)
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                Timber.e("Mapbox tile count limit exceeded: %s", limit)
            }
        })

        // Change the region state
        offlineRegion!!.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun downloadTourPackageRegion(packageId: String?, pckRegionName: String?) {
        // Reset the region selected int to 0
        regionSelected = -1
        var status = false
        try {
            // Query the DB asynchronously
            offlineManager!!.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    // Check result. If no regions have beenOfflineManager
                    // downloaded yet, notify user and return
                    if (offlineRegions == null || offlineRegions.size == 0) {
                        downloadRegion(packageId, pckRegionName)
                        return
                    }


                    for (offlineRegion in offlineRegions) {
                        regionSelected++
                        //offlineRegionsNames.add(getRegionName(offlineRegion))
                        if (packageId == getRegionName(offlineRegion)) {
                            status = true
                            break
                        }
                    }

                    if (status) {
                        //region already downloaded
                        //val items = offlineRegionsNames.toTypedArray<CharSequence>()
                        //val bounds = (offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition).bounds
                        //val regionZoom = (offlineRegions[regionSelected].definition as OfflineTilePyramidRegionDefinition).minZoom
                        val bounds = LatLngBounds.Builder()
                                .include(LatLng(mRegionToDownload?.minLatitude!!.toDouble(), mRegionToDownload?.minLongitude!!.toDouble())) // Northeast
                                .include(LatLng(mRegionToDownload?.maxLatitude!!.toDouble(), mRegionToDownload?.maxLongitude!!.toDouble())) // Southwest
                                .build()
                        // Create new camera position
                        val cameraPosition = CameraPosition.Builder()
                                .target(bounds.center)
                                .zoom(mRegionToDownload?.minZoom!!.toDouble())
                                .build()
                        // Move camera to new position
                        map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                        addTourSpotsOnMap()
                        if (isNetworkAvailable()) {
                            RetrievePackageSpots(baseContext, mRegionToDownload?.packageId!!, true).execute()
                        } else {
                            RetrievePackageSpots(baseContext, mRegionToDownload?.packageId!!, false).execute()
                        }
                    } else {
                        //download region
                        downloadRegion(packageId, pckRegionName)
                    }
                }

                override fun onError(error: String) {
                    Timber.e("Error: %s", error)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun createTourRegionMarker(number: String): Bitmap {
        val markerLayout = layoutInflater.inflate(R.layout.marker_layout, null)

        val markerImage = markerLayout.findViewById(R.id.marker_image) as ImageView
        val markerNumber = markerLayout.findViewById(R.id.marker_text) as TextView
        markerImage.setImageResource(R.mipmap.icon)

        markerNumber.text = number

        markerLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)

        val bitmap = Bitmap.createBitmap(markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerLayout.draw(canvas)
        return bitmap
    }


    private fun loadDownloadedRegion(packageId: String?, pckRegionName: String?) {
        // Reset the region selected int to 0
        regionSelected = -1
        var status = false
        // Query the DB asynchronously
        offlineManager!!.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                // Check result. If no regions have beenOfflineManager
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.size == 0) {
                    downloadRegion(packageId, pckRegionName)
                    return
                }


                for (offlineRegion in offlineRegions) {
                    regionSelected++
                    //offlineRegionsNames.add(getRegionName(offlineRegion))
                    if (packageId == getRegionName(offlineRegion)) {
                        status = true
                        break
                    }
                }

                if (status) {

                    val bounds = LatLngBounds.Builder()
                            .include(LatLng(mRegionToDownload?.minLatitude!!.toDouble(), mRegionToDownload?.minLongitude!!.toDouble())) // Northeast
                            .include(LatLng(mRegionToDownload?.maxLatitude!!.toDouble(), mRegionToDownload?.maxLongitude!!.toDouble())) // Southwest
                            .build()
                    // Create new camera position
                    val cameraPosition = CameraPosition.Builder()
                            .target(bounds.center)
                            .zoom(mRegionToDownload?.minZoom!!.toDouble())
                            .build()
                    // Move camera to new position
                    map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    addTourSpotsOnMap()
                } else {
                    //download region
                    downloadRegion(packageId, pckRegionName)
                }
            }

            override fun onError(error: String) {
                Timber.e("Error: %s", error)
            }
        })


    }

    private fun downloadedRegionList() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 1

        // Query the DB asynchronously
        offlineManager!!.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.size == 0) {
                    Toast.makeText(applicationContext, getString(R.string.toast_no_regions_yet), Toast.LENGTH_SHORT).show()
                    return
                }

                // Add all of the region names to a list
                val offlineRegionsNames = ArrayList<String>()
                for (offlineRegion in offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion))
                }
                addTourSpotsOnMap()
            }

            override fun onError(error: String) {
                Timber.e("Error: %s", error)
            }
        })
    }

    private fun getRoute(origin: Waypoint, destination: Waypoint) {
        val md = MapboxDirections.Builder()
                .setAccessToken(getString(R.string.mapbox_key))
                .setOrigin(origin)
                .setDestination(destination)
                .setProfile(DirectionsCriteria.PROFILE_WALKING)
                .build()

        md.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(response: Response<DirectionsResponse>, retrofit: Retrofit) {
                // You can get generic HTTP info about the response
                Log.d(LOG_TAG, "Response code: " + response.code())

                // Print some info about the route
                currentRoute = response.body().routes[0]
                showMessage(String.format("Route is %d meters long.", currentRoute!!.distance))

                // Draw the route on the map
                //drawRoute(currentRoute);
            }

            override fun onFailure(t: Throwable) {
                showMessage("Error: " + t.message)
            }
        })
    }


    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getRegionName(offlineRegion: OfflineRegion): String {
        // Get the region name from the offline region metadata
        var regionName: String

        //try {
        val metadata = offlineRegion.metadata
        val json = String(metadata, charset(JSON_CHARSET))
        val jsonObject = JSONObject(json)
        regionName = jsonObject.getString(JSON_FIELD_REGION_ID)
        /* } catch (exception: Exception) {
             Timber.e("Failed to decode metadata: %s", exception.message)
             regionName = String.format(getString(R.string.region_name), offlineRegion.id)
         }*/

        return regionName
    }

    // Progress bar methods
    private fun startProgress() {
        //Disable buttons
        downloadButton!!.isEnabled = false
        listButton!!.isEnabled = false

        // Start and show the progress bar
        isEndNotified = false
        progressBar!!.isIndeterminate = true
        progressBar!!.visibility = View.VISIBLE
    }

    private fun setPercentage(percentage: Int) {
        progressBar!!.isIndeterminate = false
        progressBar!!.progress = percentage
    }

    private fun endProgress(message: String) {
        // Don't notify more than once
        if (isEndNotified) {
            return
        }
        // Enable buttons
        downloadButton!!.isEnabled = true
        listButton!!.isEnabled = true

        // Stop and hide the progress bar
        isEndNotified = true
        progressBar!!.isIndeterminate = false
        progressBar!!.visibility = View.GONE

        // Show a toast
        //Toast.makeText(this@MapBoxActivity, message, Toast.LENGTH_LONG).show()
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, "Need Permission to show your current location", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    fun addTourSpotsOnMap() {

        points = ArrayList()
        tvTourName.text = mRegionToDownload?.tourNameLanguage
        map!!.setMaxZoomPreference(mRegionToDownload!!.maxZoom!!.toDouble())
        map!!.setMinZoomPreference(mRegionToDownload!!.minZoom!!.toDouble())

        mRegionRegionSpot!!.forEachIndexed { _, dataItem ->
            points.add(LatLng(dataItem.latitude!!.toDouble(), dataItem.longitude!!.toDouble()))
        }

        //ADD SPOT ON MAP
        for (i in points.indices) {
            val options = MarkerOptions()
            val iconFactory = IconFactory.getInstance(this@MapBoxActivity)
            val index = i
            val icon = iconFactory.fromBitmap(drawTextToBitmap(this@MapBoxActivity, R.drawable.ic_circle, index.plus(1).toString()))
            options.icon = icon
            options.position = points[i]
            map!!.addMarker(options)
        }

        //ATTACHED CLICKED LISTENER ON MARKER CLICK
        map!!.setOnMarkerClickListener {
            points.forEachIndexed { index, latLng ->
                if (latLng == it.position) {
                    val uri: Uri? = getFileUri(index)
                    val name: String = uri?.lastPathSegment as String
                    val extension = name.substring(name.lastIndexOf(".") + 1)
                    val data = mRegionRegionSpot?.get(index)

                    if (data?.fileType.equals("audio")) {
                        playAudio(data, uri.toString())
                    } else if (data?.fileType.equals("youtube")) {
                        if (data?.youtubeId != "") {
                            if (data?.youtubeId!!.contains("youtube")) {
                                playYoutubeVideo(data.youtubeId)
                            } else {
                                playVideoWithoutSubtitle(data.youtubeId)
                            }
                        } else showMessage(getString(R.string.txt_videoid_missing))
                    } else if (extension == "mp4") {
                        //PLAY VIDEO WITH VTT FILE
                        if (data?.fileType.equals("vtt") && data?.spotVideoVtt != "") {
                            playVideoWithSubtitle(uri.toString(), data)
                        }
                        //PLAY VIDEO WITHOUT VTT
                        if (data?.fileType.equals("video") && data?.spotVideoVtt == "") {
                            playVideoWithoutSubtitle(uri.toString())
                        }
                    } else {
                        Toast.makeText(this@MapBoxActivity, "Server Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            false
        }

        if (mRegionToDownload?.tourTime!!.isNotEmpty()) {
            //startTripTimeTracker(mRegionToDownload?.tourTime!!.toInt() * 60000).start()
        } else showMessage("Total tour time not available")

        if (mRegionToDownload?.manualPoint == "0") {
            //automatic
            drawRouteWithMapboxPoints()
        } else {
            //manual
            drawRouteWithManualPoints()
        }

        //showSpotDetails()
        dialogMsg = mRegionRegionSpot!![downloadListPosition].spotName!!.toString()
        vttFileList.clear()
        initDownload(mRegionRegionSpot!![downloadListPosition].sVideo!!.toString())
    }


    private fun drawRouteWithManualPoints() {
        try {
            if (mRegionManualRoute?.trips!!.isNotEmpty()) {
                val coordinates = mRegionManualRoute?.trips
                val point = ArrayList<LatLng>()
                coordinates!!.forEachIndexed { _, location ->
                    point.add(LatLng(location!![0]!!.toDouble(), location[1]!!.toDouble()))
                }

                map?.addPolyline(PolylineOptions()
                        .addAll(point)
                        .color(Color.parseColor("#2196F3"))
                        .width(3F)
                )
            } else {
                Toast.makeText(this@MapBoxActivity, "No manual route found", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this@MapBoxActivity, "No manual route found", Toast.LENGTH_LONG).show()
        }
    }


    private fun drawRouteWithMapboxPoints() {
        //try {
        val coordinates = mRegionRoute?.trips!![0]?.geometry?.coordinates
        val point = ArrayList<LatLng>()

        coordinates!!.forEachIndexed { _, location ->
            point.add(LatLng(
                    location!![0]!!,
                    location[1]!!))
        }

        map?.addPolyline(PolylineOptions()
                .addAll(point)
                .color(Color.parseColor("#2196F3"))
                .width(3F)
        )


    }

    private fun drawTextToBitmap(gContext: Context,
                                 gResId: Int,
                                 gText: String): Bitmap {

        val resources = gContext.resources
        val scale = resources.displayMetrics.density
        var bitmap = BitmapFactory.decodeResource(resources, gResId)

        var bitmapConfig: Bitmap.Config? = bitmap.config
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true)

        val canvas = Canvas(bitmap)
        // new antialised Paint
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // text color - #000000
        paint.color = Color.rgb(255, 255, 255)
        // text size in pixels
        paint.textSize = (14 * scale).toInt().toFloat()
        //text bold
        paint.typeface = create(DEFAULT, Typeface.BOLD)
        // text shadow
        //paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        // draw text to the Canvas center
        val bounds = Rect()
        paint.getTextBounds(gText, 0, gText.length, bounds)
        val x = (bitmap.width - bounds.width()) / 2
        val y = (bitmap.height + bounds.height()) / 2

        canvas.drawText(gText, x.toFloat(), y.toFloat(), paint)

        return bitmap
    }


    /**
     * file_type will have value either video or audio
     * spot_video_vtt contain the vtt file otherwise it will be empty
     */
    override fun onItemClickListener(pos: Int, uri: String) {
        val data = mRegionRegionSpot?.get(pos)

        //PLAY AUDIO
        if (data?.fileType.equals("audio")) {
            playAudio(data, uri)
            return
        }

        //PLAY VIDEO WITH VTT FILE
        if (data?.fileType.equals("vtt") && data?.spotVideoVtt != "") {
            playVideoWithSubtitle(uri, data)
            return
        }

        //PLAY VIDEO WITOUT VTT
        if (data?.fileType.equals("video") && data?.spotVideoVtt == "") {
            playVideoWithoutSubtitle(uri)
            return
        }

        //PLAY YOUTUBE VIDEO
        if (data?.fileType.equals("youtube") && data?.youtubeId != "") {
            if (data?.youtubeId!!.contains("youtube")) {
                playYoutubeVideo(data.youtubeId)
            } else {
                playVideoWithoutSubtitle(data.youtubeId)
            }
        } else {
            Toast.makeText(this@MapBoxActivity, getString(R.string.txt_videoid_missing), Toast.LENGTH_SHORT).show()
        }
    }


    private fun playAudio(data: DataItem?, uri: String) {
        val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mPlayTourSpot = Dialog(this, android.R.style.Theme_Translucent)
        mPlayTourSpot.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        mPlayTourSpot.setContentView(R.layout.content_play_audio)
        mPlayTourSpot.setCancelable(true)

        mPlayTourSpot.ivTourAudioCancel.setOnClickListener {
            nPlayer!!.stop()
            nPlayer!!.release()
            mPlayTourSpot.dismiss()
        }

        mPlayTourSpot.setOnKeyListener { dialog, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    nPlayer!!.stop()
                    nPlayer!!.release()
                    mPlayTourSpot.dismiss()
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                    }
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                    }
                }
            }
            return@setOnKeyListener true
        }

        mPlayTourSpot.setOnDismissListener {
            object : DialogInterface {
                override fun dismiss() {
                    if (nPlayer != null) {
                        nPlayer!!.release()
                        nPlayer = null
                    }
                }

                override fun cancel() {
                    if (nPlayer != null) {
                        nPlayer!!.release()
                        nPlayer = null
                    }
                }

            }
        }

        mPlayTourSpot.tvSpotTitle.text = data?.spotName
        mPlayTourSpot.tvSpotDesc.text = data?.spotDesc

        Picasso.get()
                .load(data!!.vThumbnail)
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(mPlayTourSpot.ivSpotImage, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {

                    }

                    override fun onError(e: Exception?) {
                        Picasso.get()
                                .load(data.vThumbnail)
                                .into(mPlayTourSpot.ivSpotImage, object : com.squareup.picasso.Callback {
                                    override fun onSuccess() {

                                    }

                                    override fun onError(e: Exception?) {
                                        print("Couldn't fetch data")
                                    }
                                })
                    }
                })


        mPlayTourSpot.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        val player: ExoPlayer
        if (nPlayer != null) {
            nPlayer!!.release()
            nPlayer = null
        }
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(this, com.google.android.exoplayer2.util.Util.getUserAgent(this, this.getString(R.string.app_name)), defaultBandwidthMeter)
        dataSourceFac = dataSourceFactory
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(defaultBandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val contentMediaSource = buildMediaSource(Uri.parse(uri))
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        player.setPlayWhenReady(true)

        mPlayTourSpot.pvSpotAudio.player = player
        player.prepare(contentMediaSource)
        nPlayer = player

        nPlayer?.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.d(TAG, "" + playbackParameters)
            }

            override fun onSeekProcessed() {
                Log.d(TAG, "")
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                Log.d(TAG, "" + trackGroups)
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.d(TAG, "" + error!!.message)
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                Log.d(TAG, "loading [$isLoading]")
            }

            override fun onPositionDiscontinuity(reason: Int) {
                Log.d(TAG, "" + reason)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Log.d(TAG, "" + repeatMode)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                Log.d(TAG, "" + shuffleModeEnabled)
            }

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                Log.d(TAG, "" + timeline)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (nPlayer != null) {
                            nPlayer!!.release()
                            nPlayer = null
                            mPlayTourSpot.dismiss()
                        }
                    }
                }
            }
        })

        hideUIwhenDialogOpen(mPlayTourSpot)
        mPlayTourSpot.show()


        // Set dialog focusable so we can avoid touching outside:
        mPlayTourSpot.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }


    private fun playVideoWithSubtitle(video: String, data: DataItem?) {
        try {
            val player: ExoPlayer
            if (nPlayer != null) {
                nPlayer!!.release()
                nPlayer = null
            }

            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mPlayTourVideo = Dialog(this, android.R.style.Theme_Translucent)
            mPlayTourVideo.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            mPlayTourVideo.setContentView(R.layout.play_tour_video)
            mPlayTourVideo.setCancelable(true)
            mPlayTourVideo.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            mPlayTourVideo.ivTourVideoCancel.setOnClickListener {
                if (nPlayer != null) {
                    nPlayer!!.stop()
                    nPlayer!!.release()
                }
                if (mPlayTourVideo.isShowing)
                    mPlayTourVideo.dismiss()
            }

            mPlayTourVideo.setOnDismissListener {
                DialogInterface.OnDismissListener {
                    hideSystemUI()
                }
            }

            mPlayTourVideo.setOnKeyListener { _, keyCode, event ->
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        if (nPlayer != null) {
                            nPlayer!!.stop()
                            nPlayer!!.release()
                        }
                        if (mPlayTourVideo.isShowing)
                            mPlayTourVideo.dismiss()
                    }
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                        }
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                        }
                    }
                }
                return@setOnKeyListener true
            }


            val defaultBandwidthMeter = DefaultBandwidthMeter()
            val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, this.getString(R.string.app_name)), defaultBandwidthMeter)
            dataSourceFac = dataSourceFactory

            val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(defaultBandwidthMeter)
            val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            val contentMediaSource = buildMediaSource(Uri.parse(video))

            val mediaSources = arrayOfNulls<MediaSource>(2) //The Size must change depending on the Uris
            mediaSources[0] = contentMediaSource //uri
            val subtitleSource = SingleSampleMediaSource(getFileUriOfVtt(data!!.spotVideoVtt),
                    dataSourceFactory, Format.createTextSampleFormat(null, MimeTypes.TEXT_VTT, Format.NO_VALUE, "en", null),
                    C.TIME_UNSET)
            mediaSources[1] = subtitleSource

            val mediaSource = MergingMediaSource(mediaSources[0], mediaSources[1])

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
            player.setPlayWhenReady(true)
            mPlayTourVideo.pvTourVideo.player = player
            player.prepare(mediaSource)
            nPlayer = player

            nPlayer?.addListener(object : Player.EventListener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                    Log.d(TAG, "" + playbackParameters)
                }

                override fun onSeekProcessed() {
                    Log.d(TAG, "")
                }

                override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    Log.d(TAG, "" + trackGroups)
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    Log.d(TAG, "" + error!!.message)
                }

                override fun onLoadingChanged(isLoading: Boolean) {
                    Log.d(TAG, "loading [$isLoading]")
                }

                override fun onPositionDiscontinuity(reason: Int) {
                    Log.d(TAG, "" + reason)
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    Log.d(TAG, "" + repeatMode)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    Log.d(TAG, "" + shuffleModeEnabled)
                }

                override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                    Log.d(TAG, "" + timeline)
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            if (nPlayer != null) {
                                nPlayer!!.release()
                                nPlayer = null
                                if (mPlayTourVideo.isShowing)
                                    mPlayTourVideo.dismiss()
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            mPlayTourVideo.pbBuffering.visibility = View.VISIBLE
                        }

                        Player.STATE_READY -> {
                            mPlayTourVideo.pbBuffering.visibility = View.GONE
                        }

                    }

                }
            })
            hideUIwhenDialogOpen(mPlayTourVideo)
            mPlayTourVideo.show()

            // Set dialog focusable so we can avoid touching outside:
            mPlayTourVideo.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        } catch (e: java.lang.Exception) {
            showMessage(e.localizedMessage)
        }
    }

    private fun hideUIwhenDialogOpen(mPlayTourVideo: Dialog) {
        mGlobalDialog = mPlayTourVideo
        mPlayTourVideo.window!!.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        mPlayTourVideo.window!!.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        //Set dialog focusable so we can avoid touching outside
        mPlayTourVideo.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        //Attached dialog dismiss listener
        mGlobalDialog.setOnDismissListener {
            //Called whenever dialog dismiss
            Log.d("ttttt", "dismisssss")
            if (nextVideoPlay.index != -1) {
                Log.d("ttttt", "inside dismiss play true")
                playVideoBasedOnUserLocation(nextVideoPlay.index)
            }
        }
    }

    override fun onBackPressed() {
        if (nPlayer != null) {
            nPlayer!!.stop()
            nPlayer!!.release()
            nPlayer = null
        }
    }


    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_BACK1) {
            onBackPressed()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun playYoutubeVideo(youtubeId: String?) {
        @SuppressLint("StaticFieldLeak") val mExtractor = object : YouTubeExtractor(this) {
            override fun onExtractionComplete(sparseArray: SparseArray<YtFile>?, videoMeta: VideoMeta) {
                if (sparseArray != null) {
                    playVideoWithoutSubtitle(sparseArray.get(18).url)
                } else {
                    showMessage("This video is no longer available on youtube")
                }
            }
        }
        mExtractor.extract(youtubeId, true, true)
    }

    private fun playVideoWithoutSubtitle(video: String) {
        try {

            val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val mPlayTourVideo = Dialog(this, android.R.style.Theme_Translucent)
            mPlayTourVideo.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            mPlayTourVideo.setContentView(R.layout.play_tour_video)
            mPlayTourVideo.setCancelable(true)
            mPlayTourVideo.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

            mPlayTourVideo.ivTourVideoCancel.setOnClickListener {
                if (nPlayer != null) {
                    nPlayer!!.stop()
                    nPlayer!!.release()
                }
                mPlayTourVideo.dismiss()
            }

            mPlayTourVideo.setOnKeyListener { dialog, keyCode, event ->
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        if (nPlayer != null) {
                            nPlayer!!.stop()
                            nPlayer!!.release()
                        }
                        mPlayTourVideo.dismiss()
                    }
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                        }
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                        }
                    }
                }

                return@setOnKeyListener true
            }

            val player: ExoPlayer
            if (nPlayer != null) {
                nPlayer!!.release()
                nPlayer = null
            }
            val defaultBandwidthMeter = DefaultBandwidthMeter()
            val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, this.getString(R.string.app_name)), defaultBandwidthMeter)
            dataSourceFac = dataSourceFactory
            val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(defaultBandwidthMeter)
            val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
            val contentMediaSource = buildMediaSource(Uri.parse(video))
            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
            player.setPlayWhenReady(true)
            mPlayTourVideo.pvTourVideo.player = player
            player.prepare(contentMediaSource)
            nPlayer = player

            nPlayer?.addListener(object : Player.EventListener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                    Log.d(TAG, "" + playbackParameters)
                }

                override fun onSeekProcessed() {
                    Log.d(TAG, "")
                }

                override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    Log.d(TAG, "" + trackGroups)
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    Log.d(TAG, "" + error!!.message)
                }

                override fun onLoadingChanged(isLoading: Boolean) {
                    Log.d(TAG, "loading [$isLoading]")
                }

                override fun onPositionDiscontinuity(reason: Int) {
                    Log.d(TAG, "" + reason)
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    Log.d(TAG, "" + repeatMode)
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    Log.d(TAG, "" + shuffleModeEnabled)
                }

                override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                    Log.d(TAG, "" + timeline)
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            if (nPlayer != null) {
                                nPlayer!!.release()
                                nPlayer = null
                                mPlayTourVideo.dismiss()
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            mPlayTourVideo.pbBuffering.visibility = View.VISIBLE
                        }

                        Player.STATE_READY -> {
                            mPlayTourVideo.pbBuffering.visibility = View.GONE
                        }

                    }
                }
            })

            hideUIwhenDialogOpen(mPlayTourVideo)
            mPlayTourVideo.show()

            mPlayTourVideo.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private val playVideo: (String) -> Unit = {
        val mPlayTourVideo = Dialog(this, android.R.style.Theme_Translucent)
        mPlayTourVideo.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        mPlayTourVideo.setContentView(R.layout.play_tour_video)
        mPlayTourVideo.setCancelable(true)

        mPlayTourVideo.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        val player: ExoPlayer
        if (nPlayer != null) {
            nPlayer!!.release()
            nPlayer = null
        }
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, this.getString(R.string.app_name)), defaultBandwidthMeter)
        dataSourceFac = dataSourceFactory
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(defaultBandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val contentMediaSource = buildMediaSource(Uri.parse(it))
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        player.setPlayWhenReady(true)
        mPlayTourVideo.pvTourVideo.player = player
        player.prepare(contentMediaSource)
        nPlayer = player

        nPlayer?.addListener(object : Player.EventListener {
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                Log.d(TAG, "" + playbackParameters)
            }

            override fun onSeekProcessed() {
                Log.d(TAG, "")
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                Log.d(TAG, "" + trackGroups)
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                Log.d(TAG, "" + error!!.message)
            }

            override fun onLoadingChanged(isLoading: Boolean) {
                Log.d(TAG, "loading [$isLoading]")
            }

            override fun onPositionDiscontinuity(reason: Int) {
                Log.d(TAG, "" + reason)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                Log.d(TAG, "" + repeatMode)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                Log.d(TAG, "" + shuffleModeEnabled)
            }

            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                Log.d(TAG, "" + timeline)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        if (nPlayer != null) {
                            nPlayer!!.release()
                            nPlayer = null
                            mPlayTourVideo.dismiss()
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        mPlayTourVideo.pbBuffering.visibility = View.VISIBLE
                    }

                    Player.STATE_READY -> {
                        mPlayTourVideo.pbBuffering.visibility = View.GONE
                    }

                }
            }
        })
        hideUIwhenDialogOpen(mPlayTourVideo)
        mPlayTourVideo.show()
        mPlayTourVideo.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
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
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    private fun showSpotDetails() {

        val downloadList: ArrayList<String> = ArrayList()
        mRegionRegionSpot!!.forEachIndexed { index, e ->
            downloadList.add(mRegionRegionSpot!![index].sVideo!!)
        }

        rvSpotList!!.addItemDecoration(DividerItemDecoration(this@MapBoxActivity, LinearLayoutManager.HORIZONTAL))
        spotListAdapter = SpotListAdapter(applicationContext, this.mRegionRegionSpot!!, playVideo)
        spotListAdapter?.onItemClickedListener(this)
        val horizontalLayoutManager = LinearLayoutManager(this@MapBoxActivity, LinearLayoutManager.HORIZONTAL, false)
        rvSpotList!!.layoutManager = horizontalLayoutManager
        rvSpotList!!.adapter = spotListAdapter


    }


    companion object {
        private val TAG = "OffManActivity"

        // JSON encoding/decoding
        val JSON_CHARSET = "UTF-8"
        val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
        val JSON_FIELD_REGION_ID = "FIELD_REGION_ID"
        var nPlayer: ExoPlayer? = null

    }


    //HANDLING ROOM DATABASE
    @SuppressLint("StaticFieldLeak")
    inner class InsertPackageSpots// only retain a weak reference to the activity
    internal constructor(val packageId: String, val myDataset: String, val context: Context) : AsyncTask<Void, Void, Boolean>() {

        // doInBackground methods runs on a worker thread
        override fun doInBackground(vararg objs: Void): Boolean? {

            var packageSpots = PackageSpots(packageId, myDataset)

            //adding to database
            DatabaseClient.getInstance(context)
                    .appDatabase
                    .tourPackageDao()
                    .insertPackageSpots(packageSpots)

            return true
        }

        // onPostExecute runs on main thread
        override fun onPostExecute(bool: Boolean?) {

        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class UpdatePackageSpots// only retain a weak reference to the activity
    internal constructor(val packageId: String, val myDataset: String, val context: Context) : AsyncTask<Void, Void, Boolean>() {

        // doInBackground methods runs on a worker thread
        override fun doInBackground(vararg objs: Void): Boolean? {

            val packageSpots = PackageSpots(packageId, myDataset)

            /*var cat: CategoriesTable = CategoriesTable(myDataset.get(j).catName.toString(),
                    myDataset.get(j).catColor.toString()
                    , myDataset.get(j).catValue.toString(), myDataset.get(j).icon.toString(), false)*/


            //adding to database
            DatabaseClient.getInstance(context)
                    .appDatabase
                    .tourPackageDao()
                    .updatePackageSpots(packageId, packageSpotsListStr)

            return true
        }

        // onPostExecute runs on main thread
        override fun onPostExecute(bool: Boolean?) {
            if (bool!!) {
            }
        }
    }

    inner class RetrievePackageSpots// only retain a weak reference to the activity
    internal constructor(@SuppressLint("StaticFieldLeak") val context: Context, val packageSpotId: String, val internetStatus: Boolean) : AsyncTask<Void, Void, List<PackageSpots>>() {

        override fun doInBackground(vararg voids: Void): List<PackageSpots> {
            return DatabaseClient
                    .getInstance(context)
                    .appDatabase
                    .tourPackageDao()
                    .getPackageSpots(packageSpotId)

        }

        override fun onPostExecute(data: List<PackageSpots>) {
            super.onPostExecute(data)
            if (data.isNotEmpty()) {
                if (internetStatus) {
                    //fresh data available, update the database
                    UpdatePackageSpots(mRegionToDownload?.packageId!!, packageSpotsListStr, baseContext).execute()
                } else {
                    //package exist in database
                    val gson = Gson()
                    val spots = gson.fromJson(data[0].packageSpots, PackageSpotsResponse::class.java)
                    mRegionRegionSpot = spots.data as List<DataItem>
                    mRegionToDownload = spots.mapData
                    mRegionManualRoute = spots.manual!![0]
                    mRegionRoute = spots.route
                    loadDownloadedRegion(spots.mapData!!.packageId, spots.mapData.regionName)
                }
            } else {
                if (packageSpotsListStr != "") {
                    InsertPackageSpots(mRegionToDownload?.packageId!!, packageSpotsListStr, baseContext).execute()
                } else {
                    showMessage(getString(R.string.txt_dwnld_tour_spots))
                }
            }

        }
    }


    var vttFileList = mutableListOf<String>()

    /**
     *CODE FOR DOWNLOADING FILES AUDIO,VIDEO,VTT AND SO ON
     */
    fun initDownload(url: String) {
        val path = Environment.getExternalStorageDirectory().toString() + File.separator + "TourGuide/"
        val directory = File(path)
        val files = directory.listFiles()
        val fileName = url.substring(url.lastIndexOf('/') + 1)
        val mFileNameList = mutableListOf<String>()
        if (files != null) {
            files.forEachIndexed { index, file ->
                mFileNameList.add(file.name)
            }
            if (mFileNameList.contains(fileName)) {
                print("FILE PRESENT INSIDE FOLDER")
                if (downloadListPosition < mRegionRegionSpot!!.size - 1) {
                    downloadListPosition = downloadListPosition.plus(1)
                    dialogMsg = mRegionRegionSpot!![downloadListPosition].spotName!!.toString()
                    initDownload(mRegionRegionSpot!![downloadListPosition].sVideo.toString())
                } else {
                    for (data in mRegionRegionSpot!!) {
                        if (data.spotVideoVtt!!.isNotEmpty()) {
                            vttFileList.add(data.spotVideoVtt)
                        }
                    }
                    if (vttFileList.size > 0) {
                        //downloading vtt files
                        initDownloadVtt(vttFileList[0])
                    } else {
                        //download thumbnail
                        downloadListPosition = 0
                        initDownloadImage(mRegionRegionSpot!![downloadListPosition].vThumbnail.toString())
                    }
                }
            } else {
                print("FILE NOT PRESENT INSIDE FOLDER")
                dialogMsg = mRegionRegionSpot!![downloadListPosition].spotName!!.toString()
                DownloadTask().execute(url)
            }
        } else {
            DownloadTask().execute(url)
        }
    }


    /**
     * Method to handle image download
     */
    private fun initDownloadImage(url: String) {
        val path = Environment.getExternalStorageDirectory().toString() + File.separator + "TourGuide/" + "Images/"
        val directory = File(path)
        val files = directory.listFiles()
        val fileName = url.substring(url.lastIndexOf('/') + 1)
        val mFileNameList = mutableListOf<String>()
        if (files != null) {
            files.forEachIndexed { index, file ->
                mFileNameList.add(file.name)
            }
            if (mFileNameList.contains(fileName)) {
                print("FILE PRESENT INSIDE FOLDER")
                if (downloadListPosition < mRegionRegionSpot!!.size - 1) {
                    downloadListPosition = downloadListPosition.plus(1)
                    dialogMsg = mRegionRegionSpot!![downloadListPosition].spotName!!.toString()
                    initDownloadImage(mRegionRegionSpot!![downloadListPosition].vThumbnail.toString())
                } else {
                    showSpotDetails()
                }
            } else {
                print("FILE NOT PRESENT INSIDE FOLDER")
                dialogMsg = mRegionRegionSpot!![downloadListPosition].spotName!!.toString()
                DownloadTaskImage().execute(url)
            }
        } else {
            DownloadTaskImage().execute(url)
        }
    }

    /**
     * Method to handle vtt file download
     */
    private fun initDownloadVtt(url: String) {
        val path = Environment.getExternalStorageDirectory().toString() + File.separator + "TourGuide/"
        val directory = File(path)
        val files = directory.listFiles()
        val fileName = url.substring(url.lastIndexOf('/') + 1)
        val mFileNameList = mutableListOf<String>()
        if (files != null) {
            files.forEachIndexed { index, file ->
                mFileNameList.add(file.name)
            }
            if (mFileNameList.contains(fileName)) {
                print("FILE PRESENT INSIDE FOLDER")
                if (downloadVttListPosition < vttFileList.size - 1) {
                    downloadVttListPosition = downloadVttListPosition.plus(1)
                    dialogMsgVtt = mRegionRegionSpot!![downloadVttListPosition].spotName!!.toString() + " VTT"
                    initDownloadVtt(mRegionRegionSpot!![downloadVttListPosition].spotVideoVtt.toString())
                } else {
                    downloadListPosition = 0
                    initDownloadImage(mRegionRegionSpot!![downloadListPosition].vThumbnail.toString())
                }
            } else {
                print("FILE NOT PRESENT INSIDE FOLDER")
                dialogMsgVtt = mRegionRegionSpot!![downloadVttListPosition].spotName!!.toString() + " VTT"
                DownloadTaskVtt().execute(url)
            }
        } else {
            DownloadTaskVtt().execute(url)
        }
    }

    /**
     * Method to download video
     */
    @SuppressLint("StaticFieldLeak")
    inner class DownloadTask : AsyncTask<String, String, String>() {

        private var progressDialog: ProgressDialog? = null
        private var fileName: String? = null
        private var folder: String? = null

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         */
        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = ProgressDialog(this@MapBoxActivity)
            progressDialog!!.setTitle(getString(R.string.txt_downloading))
            progressDialog!!.setMessage(dialogMsg)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        override fun doInBackground(vararg f_url: String?): String {
            try {
                var count: Int = 0
                val url = URL(f_url[0])
                val connection = url.openConnection()
                connection.connect()
                // getting file length
                val lengthOfFile = connection.contentLength


                // input stream to read file - with 8k buffer
                val input = BufferedInputStream(url.openStream(), 8192)

                val timestamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())

                //Extract file name from URL
                fileName = f_url[0]?.substring(f_url[0]!!.lastIndexOf('/') + 1, f_url[0]!!.length)
                folder = Environment.getExternalStorageDirectory().toString() + File.separator + "TourGuide/"

                //Create TourGuide folder if it does not exist
                val directory = File(folder!!)

                if (!directory.exists()) {
                    directory.mkdirs()
                }

                // Output stream to write file
                val output = FileOutputStream(folder!! + fileName!!)

                val data = ByteArray(1024)

                var total: Long = 0
                //while ((count = input.read(data)) != -1) {
                while (count != -1) {
                    count = input.read(data)
                    total += count.toLong()
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (total * 100 / lengthOfFile).toInt())
                    Log.d(TAG, "Progress: " + (total * 100 / lengthOfFile).toInt())

                    // writing data to file
                    output.write(data, 0, count)
                }

                // flushing output
                output.flush()

                // closing streams
                output.close()
                input.close()
                return "Downloaded at: $folder$fileName"

            } catch (e: Exception) {
                Log.e("Error: ", e.message)
            }

            return "Something went wrong"
        }

        override fun onPostExecute(message: String?) {
            // dismiss the dialog after the file was downloaded
            progressDialog!!.dismiss()
            if (downloadListPosition != mRegionRegionSpot!!.size - 1) {
                downloadListPosition = downloadListPosition.plus(1)
                initDownload(mRegionRegionSpot!![downloadListPosition].sVideo.toString())
            } else {
                for (data in mRegionRegionSpot!!) {
                    if (data.spotVideoVtt!!.isNotEmpty()) {
                        vttFileList.add(data.spotVideoVtt)
                    }
                }
                if (vttFileList.size > 0) {
                    //downloading vtt files
                    initDownloadVtt(vttFileList[0])
                } else {
                    //download thumbnail
                    downloadListPosition = 0
                    initDownloadImage(mRegionRegionSpot!![downloadListPosition].vThumbnail.toString())
                }
            }

        }

        override fun onProgressUpdate(vararg progress: String?) {
            progressDialog!!.progress = Integer.parseInt(progress[0]!!)
        }
    }


    /**
     * Method to download vtt file
     */
    @SuppressLint("StaticFieldLeak")
    inner class DownloadTaskVtt : AsyncTask<String, String, String>() {
        private var progressDialog: ProgressDialog? = null
        private var fileName: String? = null
        private var folder: String? = null

        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = ProgressDialog(this@MapBoxActivity)
            progressDialog!!.setTitle("Downloading VTT Files")
            progressDialog!!.setMessage(dialogMsgVtt)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        override fun doInBackground(vararg f_url: String?): String {
            var count: Int = 0
            try {
                val url = URL(f_url[0])
                val connection = url.openConnection()
                connection.connect()
                // getting file length
                val lengthOfFile = connection.contentLength


                // input stream to read file - with 8k buffer
                val input = BufferedInputStream(url.openStream(), 8192)
                //Extract file name from URL
                fileName = f_url[0]?.substring(f_url[0]!!.lastIndexOf('/') + 1, f_url[0]!!.length)
                folder = Environment.getExternalStorageDirectory().toString() + File.separator + "TourGuide/"
                //Create TourGuide folder if it does not exist
                val directory = File(folder!!)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                // Output stream to write file
                val output = FileOutputStream(folder!! + fileName!!)
                val data = ByteArray(1024)
                var total: Long = 0
                while (count != -1) {
                    count = input.read(data)
                    total += count.toLong()
                    publishProgress("" + (total * 100 / lengthOfFile).toInt())
                    output.write(data, 0, count)
                }

                // flushing output
                output.flush()

                // closing streams
                output.close()
                input.close()
                return "Downloaded at: $folder$fileName"

            } catch (e: Exception) {
                Log.e("Error: ", e.message)
            }

            return "Something went wrong"
        }

        override fun onPostExecute(message: String?) {
            progressDialog!!.dismiss()
            if (downloadVttListPosition != vttFileList.size - 1) {
                downloadVttListPosition = downloadVttListPosition.plus(1)
                initDownloadVtt(vttFileList[downloadVttListPosition])
            } else {
                downloadListPosition = 0
                initDownloadImage(mRegionRegionSpot!![downloadListPosition].vThumbnail.toString())
            }
        }

        override fun onProgressUpdate(vararg progress: String?) {
            progressDialog!!.progress = Integer.parseInt(progress[0]!!)
        }
    }


    /**
     * Method to download image
     */
    @SuppressLint("StaticFieldLeak")
    inner class DownloadTaskImage : AsyncTask<String, String, String>() {
        private var progressDialog: ProgressDialog? = null
        private var fileName: String? = null
        private var folder: String? = null

        override fun onPreExecute() {
            super.onPreExecute()
            progressDialog = ProgressDialog(this@MapBoxActivity)
            progressDialog!!.setTitle("Downloading Thumbnail")
            progressDialog!!.setMessage(dialogMsg)
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        override fun doInBackground(vararg f_url: String?): String {
            var count: Int = 0
            try {
                val url = URL(f_url[0])
                val connection = url.openConnection()
                connection.connect()
                // getting file length
                val lengthOfFile = connection.contentLength


                // input stream to read file - with 8k buffer
                val input = BufferedInputStream(url.openStream(), 8192)
                //Extract file name from URL
                fileName = f_url[0]?.substring(f_url[0]!!.lastIndexOf('/') + 1, f_url[0]!!.length)
                folder = Environment.getExternalStorageDirectory().toString() + File.separator + "TourGuide/" + "Images/"
                //Create TourGuide folder if it does not exist
                val directory = File(folder!!)
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                // Output stream to write file
                val output = FileOutputStream(folder!! + fileName!!)
                val data = ByteArray(1024)
                var total: Long = 0
                while (count != -1) {
                    count = input.read(data)
                    total += count.toLong()
                    publishProgress("" + (total * 100 / lengthOfFile).toInt())
                    output.write(data, 0, count)
                }

                // flushing output
                output.flush()

                // closing streams
                output.close()
                input.close()
                return "Downloaded at: $folder$fileName"

            } catch (e: Exception) {
                Log.e("Error: ", e.message)
            }

            return "Something went wrong"
        }

        override fun onPostExecute(message: String?) {
            progressDialog!!.dismiss()
            if (downloadListPosition != mRegionRegionSpot!!.size - 1) {
                downloadListPosition = downloadListPosition.plus(1)
                initDownloadImage(mRegionRegionSpot!![downloadListPosition].vThumbnail.toString())
            } else {
                showSpotDetails()
            }
        }

        override fun onProgressUpdate(vararg progress: String?) {
            progressDialog!!.progress = Integer.parseInt(progress[0]!!)
        }
    }


    /**
     * Method to return file uri
     */
    private fun getFileUri(position: Int): Uri {
        val fileName = mRegionRegionSpot?.get(position)?.sVideo!!.substring(mRegionRegionSpot!![position].sVideo!!.lastIndexOf('/') + 1)
        val path = Environment.getExternalStorageDirectory().absolutePath + File.separator + "TourGuide/$fileName"
        val file = File(path)
        val uri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            uri = Uri.parse(file.path)
        } else {
            uri = Uri.fromFile(file)
        }
        return uri
    }

    private fun getFileUriOfVtt(path: String?): Uri {
        val fileName = path!!.substring(path.lastIndexOf('/') + 1)
        val path = Environment.getExternalStorageDirectory().absolutePath + File.separator + "TourGuide/$fileName"
        val file = File(path)
        val uri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            uri = Uri.parse(file.path)
        } else {
            uri = Uri.fromFile(file)
        }
        return uri
    }

}
