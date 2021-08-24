package com.app.tourguide.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.app.tourguide.R
import com.app.tourguide.base_classes.BaseActivity
import com.app.tourguide.ui.avaliableplaces.AvailableLocationFragment
import com.app.tourguide.ui.language.ChooseLanguageFragment
import com.app.tourguide.ui.mapBox.MapBoxActivity
import com.app.tourguide.ui.mapBox.response.TourTimePojo
import com.app.tourguide.ui.placedetail.PlacesDetailsFragment
import com.app.tourguide.ui.tourLanguage.TourFragment
import com.app.tourguide.ui.videoView.VideoViewFragment
import com.app.tourguide.utils.Constants
import com.app.tourguide.utils.saveValue
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_view.*
import java.util.concurrent.TimeUnit


class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {


    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var fragment: Fragment


    override fun onBackStackChanged() {
        val fragment = supportFragmentManager.findFragmentById(R.id.container_main)
        if (fragment is VideoViewFragment) {
            iv_toggle_menu.visibility = View.INVISIBLE
        } else {
            iv_toggle_menu.visibility = View.VISIBLE
        }


        tv_end_tour.visibility = when (Preferences.prefs!!.getString(Constants.END_TIME, "") == "") {
            true -> View.GONE
            false -> View.VISIBLE
        }

    }

    override fun getID(): Int {
        return R.layout.activity_main
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.addOnBackStackChangedListener(this)

    }


    override fun iniView(savedInstanceState: Bundle?) {
        initViews()
    }


    private fun initViews() {
        mDrawerLayout = findViewById(R.id.drawer_layout)
        tv_language.setOnClickListener {
            fragment = supportFragmentManager?.findFragmentById(R.id.container_main)!!
            val fragmentChoose = ChooseLanguageFragment()
            val args = Bundle()
            args.putBoolean(Constants.PREVIEW_STATUS, false)
            fragmentChoose.arguments = args
            if (fragment != null && fragment is ChooseLanguageFragment) {
                var chooseLang = fragment as ChooseLanguageFragment
                chooseLang.updateView()
            } else {
                addFragment(fragmentChoose, true, R.id.container_main)
            }
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }

        tv_tour_list.setOnClickListener {
            fragment = supportFragmentManager?.findFragmentById(R.id.container_main)!!
            if (fragment != null && (fragment is ChooseLanguageFragment || fragment is PlacesDetailsFragment || fragment is TourFragment))
                supportFragmentManager?.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            mDrawerLayout.closeDrawer(GravityCompat.START)
        }


        mDrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(p0: Int) {
            }

            override fun onDrawerSlide(p0: View, p1: Float) {
            }

            override fun onDrawerClosed(p0: View) {
                iv_toggle_menu.visibility = View.VISIBLE
            }

            override fun onDrawerOpened(p0: View) {
                iv_toggle_menu.visibility = View.GONE
            }
        })

        iv_toggle_menu.setOnClickListener {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START)
                iv_toggle_menu.visibility = View.VISIBLE
            } else {
                iv_toggle_menu.visibility = View.GONE
                mDrawerLayout.openDrawer(GravityCompat.START)
            }
        }

        iv_closedrawer.setOnClickListener {
            mDrawerLayout.closeDrawer(GravityCompat.START)
            iv_toggle_menu.visibility = View.VISIBLE
        }

        tv_end_tour.setOnClickListener {
            mDrawerLayout.closeDrawer(GravityCompat.START)
            Preferences.prefs!!.saveValue(Constants.END_TIME, "")
            tv_end_tour.visibility = View.GONE
        }

        iv_toggle_menu.visibility = View.VISIBLE

        tv_end_tour.visibility = when (Preferences.prefs!!.getString(Constants.END_TIME, "") == "") {
            true -> View.GONE
            false -> View.VISIBLE
        }


        when (intent.getStringExtra("DESTINATION")) {
            "language" -> {
                //replaceFragment(AvailableLocationFragment(), R.id.container_main)
                val fragmentChoose = ChooseLanguageFragment()
                val args = Bundle()
                args.putBoolean(Constants.PREVIEW_STATUS, false)
                fragmentChoose.arguments = args
                addFragment(fragmentChoose, true, R.id.container_main)
            }
            "tourlist" -> {
                replaceFragment(AvailableLocationFragment(), R.id.container_main)
            }
            else -> {
                checkWhetherTourRunningOrNot()
            }
        }

    }


    private fun checkWhetherTourRunningOrNot() {
        val gson = Gson()
        val timerJsonData = Preferences.prefs!!.getString(Constants.END_TIME, "")
        if (timerJsonData.isNotEmpty()) {
            val timerPojo: TourTimePojo = gson.fromJson(timerJsonData, TourTimePojo::class.java)
            if (timerPojo.doubleTourStatus) {
                //execute below code if current selected package_id matched with tour stored in preferences
                val currentTime = System.currentTimeMillis()
                val endTime = timerPojo.endTourTime
                val diff = endTime - currentTime
                val minuteLeft = TimeUnit.MILLISECONDS.toMinutes(diff)
                val secondLeft = TimeUnit.MILLISECONDS.toSeconds(diff)
                //minuteLeft > 0 && secondLeft > 0 &&
                if (!timerPojo.statusInactivation) {
                    //As tour minutes left, user can continue the tour without entering the token
                    startActivity(Intent(this, MapBoxActivity::class.java).apply {
                        putExtra("packageId", timerPojo.packageId)
                        putExtra("packageTokenId", timerPojo.tourTokenId)
                        putExtra("tourTimeLeft", minuteLeft)
                        putExtra("comingFrom", "available_location_fragment")
                        putExtra("doubleTourStatus", true)
                    })
                }
            } else {
                val intent = Intent(this, MapBoxActivity::class.java)
                startActivity(intent.apply {
                    putExtra("packageId", timerPojo.packageId)
                    putExtra("packageTokenId", "")
                    putExtra("comingFrom", "available_location_fragment")
                    putExtra("doubleTourStatus", false)
                })
            }
        } else {
            replaceFragment(AvailableLocationFragment(), R.id.container_main)
        }
    }

}



