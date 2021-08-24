package com.app.tourguide.ui.avaliableplaces

import Preferences
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.app.tourguide.R
import com.app.tourguide.base_classes.BaseFragment
import com.app.tourguide.database.DatabaseClient
import com.app.tourguide.database.entity.TourPackage
import com.app.tourguide.expandlelist.ExpandableListViewAdapter
import com.app.tourguide.ui.avaliableplaces.model.DataItem
import com.app.tourguide.ui.avaliableplaces.model.ResponseAvailableTour
import com.app.tourguide.ui.mapBox.MapBoxActivity
import com.app.tourguide.ui.mapBox.response.TourTimePojo
import com.app.tourguide.ui.tourLanguage.TourFragment
import com.app.tourguide.utils.Constants
import com.app.tourguide.utils.saveValue
import com.app.tourguide.utils.security.ApiFailureTypes
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_availablelocation.*
import java.util.concurrent.TimeUnit


class AvailableLocationFragment : BaseFragment() {

    private var expandableListViewAdapter: ExpandableListViewAdapter? = null
    private lateinit var conext: Context

    private var placesList: ArrayList<DataItem> = ArrayList()
    private var mViewModel: AvaliablePlacesModel? = null
    private val prevExpandPosition = intArrayOf(-1)
    private val gson = Gson()
    private var placeListStr: String = ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        conext = (activity as Context?)!!
        mViewModel = ViewModelProviders.of(this).get(AvaliablePlacesModel::class.java)
        return inflater.inflate(R.layout.fragment_availablelocation, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        mViewModel?.postDeviceToken(deviceToken(), getMacAddress())
    }


    override fun onResume() {
        super.onResume()
        if (isNetworkAvailable(view)) {
            attachObserversToGetPlacesList()
        } else {
            RetrieveAllTourPackage(activity!!).execute()
        }
    }


    private fun initListeners() {
        val gson = Gson()
        val timerJsonData = Preferences.prefs!!.getString(Constants.END_TIME, "")

        expandableListViewAdapter = ExpandableListViewAdapter(conext, placesList)
        rv_availableplaces.setAdapter(expandableListViewAdapter)

        rv_availableplaces!!.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            Preferences.initPreferences(activity!!.applicationContext)
            if (timerJsonData != "") {
                //retrieve tour timer data stored in the database
                val timerPojo: TourTimePojo = gson.fromJson(timerJsonData, TourTimePojo::class.java)
                if (timerPojo.packageId == placesList[groupPosition].tourPackages!![childPosition]!!.id) {
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
                            startActivity(Intent(activity, MapBoxActivity::class.java).apply {
                                putExtra("packageId", placesList[groupPosition].tourPackages!![childPosition]!!.id)
                                putExtra("packageTokenId", timerPojo.tourTokenId)
                                putExtra("tourTimeLeft", minuteLeft)
                                putExtra("comingFrom", "available_location_fragment")
                                putExtra("doubleTourStatus", true)
                            })
                        } else {
                            //As tour minutes end, user need to enter the token to continue the tour
                            goToTourDetail(placesList[groupPosition].tourPackages!![childPosition]!!.id.toString())
                        }
                    } else {
                        val intent = Intent(activity, MapBoxActivity::class.java)
                        startActivity(intent.apply {
                            putExtra("packageId", timerPojo.packageId)
                            putExtra("packageTokenId", timerPojo.tourTokenId)
                            putExtra("comingFrom", "available_location_fragment")
                            putExtra("doubleTourStatus", false)
                        })
                    }
                } else {
                    //package_id not matched with the id stored in storage then go to tour details
                    goToTourDetail(placesList[groupPosition].tourPackages!![childPosition]!!.id.toString())
                }


            } else {
                goToTourDetail(placesList[groupPosition].tourPackages!![childPosition]!!.id.toString())
            }
            false
        }

        rv_availableplaces.setOnGroupExpandListener { groupPosition ->
            if (prevExpandPosition[0] >= 0 && prevExpandPosition[0] !== groupPosition) {
                rv_availableplaces.collapseGroup(prevExpandPosition[0])
            }
            prevExpandPosition[0] = groupPosition
        }

        // ExpandableListView Group collapsed listener
        rv_availableplaces.setOnGroupCollapseListener { groupPosition ->
        }

    }


    private fun goToTourDetail(packageId: String) {
        val fragment = TourFragment()
        fragment.arguments = Bundle().apply {
            putString(Constants.PACKAGE_ID, packageId)
            putBoolean(Constants.MAP_STATUS, false)
        }
        addFragment(fragment, true, R.id.container_main)
    }


    private fun attachObserversToGetPlacesList() {
        mViewModel?.responsePlaces?.observe(this, Observer {
            it?.let {
                if (it.status == 1) {
                    placesList = it.data as ArrayList<DataItem>

                    placeListStr = gson.toJson(it)

                    expandableListViewAdapter = ExpandableListViewAdapter(conext, placesList)
                    rv_availableplaces.setAdapter(expandableListViewAdapter)
                    expandableListViewAdapter?.notifyDataSetChanged()


                    //RetrieveAllTourPackage(activity!!).execute()
                }
            }
        })

        mViewModel?.postDeviceTokenResp?.observe(this, Observer
        {
            it?.let {
                if (it.status == 1) {
                    Preferences.prefs?.saveValue(Constants.IS_DEVICE_ID_SEND, true)
                    mViewModel?.getPackagesData(deviceToken())
                }
            }
        })

        mViewModel?.apiError?.observe(this, Observer {
            it?.let {
                showSnackBar(it)
            }
        })

        mViewModel?.isLoading?.observe(this, Observer {
            it?.let { showLoading(it) }
        })

        mViewModel?.onFailure?.observe(this, Observer {
            it?.let {
                showSnackBar(ApiFailureTypes().getFailureMessage(it))
            }
        })

    }

    inner class InsertAllTourPackage// only retain a weak reference to the activity
    internal constructor(val myDataset: String, val context: Context) : AsyncTask<Void, Void, Boolean>() {

        // doInBackground methods runs on a worker thread
        override fun doInBackground(vararg objs: Void): Boolean? {
            val tourPackage = TourPackage("1", myDataset)
            //adding to database
            DatabaseClient.getInstance(context).appDatabase.tourPackageDao().insert(tourPackage)
            return true
        }

        // onPostExecute runs on main thread
        override fun onPostExecute(bool: Boolean?) {
            if (bool!!) {

            }
        }

    }


    inner class UpdateAllTourPackage// only retain a weak reference to the activity
    internal constructor(val myDataset: String, val context: Context) : AsyncTask<Void, Void, Boolean>() {

        // doInBackground methods runs on a worker thread
        override fun doInBackground(vararg objs: Void): Boolean? {
            //adding to database
            DatabaseClient
                    .getInstance(context).appDatabase
                    .tourPackageDao()
                    .update("1", placeListStr)

            return true
        }

        // onPostExecute runs on main thread
        override fun onPostExecute(bool: Boolean?) {
            if (bool!!) {
            }
        }
    }

    inner class RetrieveAllTourPackage// only retain a weak reference to the activity
    internal constructor(@SuppressLint("StaticFieldLeak") val context: Context) : AsyncTask<Void, Void, List<TourPackage>>() {

        override fun doInBackground(vararg voids: Void): List<TourPackage> {
            return DatabaseClient
                    .getInstance(context)
                    .appDatabase
                    .tourPackageDao()
                    .getAll()
        }

        override fun onPostExecute(tasks: List<TourPackage>?) {
            super.onPostExecute(tasks)
            if (tasks!!.size != 0) {
                //run update
                if (placeListStr != "") {
                    UpdateAllTourPackage(placeListStr, activity!!)
                }

                val gson = Gson()
                val data = gson.fromJson(tasks.get(0).spots, ResponseAvailableTour::class.java)

                placesList = data.data as ArrayList<DataItem>

                expandableListViewAdapter = ExpandableListViewAdapter(context, placesList)
                rv_availableplaces.setAdapter(expandableListViewAdapter)
                expandableListViewAdapter?.notifyDataSetChanged()
            } else {
                //run insert
                if (placeListStr != "") {
                    InsertAllTourPackage(placeListStr, activity!!).execute()
                }
            }
        }
    }
}

