package com.app.tourguide.base_classes


import Preferences
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.app.tourguide.R
import com.app.tourguide.callBack.AlertDialogListener
import com.app.tourguide.utils.Constants
import com.app.tourguide.utils.Utils
import com.app.tourguide.utils.getValue
import com.app.tourguide.utils.security.ApiFailureTypes
import com.google.android.material.snackbar.Snackbar
import permission.auron.com.marshmallowpermissionhelper.FragmentManagePermission
import java.util.*

/**
 * Created by android on 2/11/17.
 * *
 */
open class BaseFragment : FragmentManagePermission() {
    protected val TAG = javaClass.simpleName
    protected var mContent: View? = null// For showing snackbar
    private var mActivity: FragmentActivity? = null

    private var mProgressDialog: Dialog? = null
    private lateinit var mCalendar: Calendar
    private var mStartTime: Calendar? = null
    private var mEndTime: Calendar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContent = view
        mStartTime = Calendar.getInstance()
        mEndTime = Calendar.getInstance()
        mCalendar = Calendar.getInstance()
    }


    @SuppressLint("WrongConstant")
    fun showSnackBar(message: String) {
        mContent?.let {
            val snackbar = Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            val tv = snackbarView.findViewById<TextView>(R.id.snackbar_text)
            tv.maxLines = 3
            snackbar.duration = Constants.SNACK_BAR_DURATION
            snackbar.show()
        }
    }


    fun changeAppLanguage(type: String) {
        val locale = Locale(type)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onPause() {
        super.onPause()
        Utils.hideKeyboard(this.activity!!)
    }

    override fun onStart() {
        super.onStart()
        Utils.hideKeyboard(activity!!)
    }

    /**
     * to get device Id
     */
    @SuppressLint("HardwareIds")
    fun deviceToken(): String {
        return Settings.Secure.getString(activity?.contentResolver, Settings.Secure.ANDROID_ID)
    }


    @SuppressLint("HardwareIds")
    fun getMacAddress(): String {
        val mWifiManager: WifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return mWifiManager.connectionInfo.macAddress
    }

    fun isDeviceIdSend(): Boolean? {
        return Preferences.prefs?.getValue(Constants.IS_DEVICE_ID_SEND, false)
    }


    /**
     * Add fragment with or without addToBackStack
     *
     * @param fragment       which needs to be attached
     * @param addToBackStack is fragment needed to backstack
     */
    fun addFragment(fragment: Fragment, addToBackStack: Boolean, id: Int) {
        val tag = fragment.javaClass.simpleName
        val fragmentManager = mActivity?.supportFragmentManager
        val fragmentOldObject = fragmentManager?.findFragmentByTag(tag)
        val transaction = fragmentManager?.beginTransaction()
        transaction?.setCustomAnimations(R.anim.anim_in, R.anim.anim_out, R.anim.anim_in_reverse, R.anim.anim_out_reverse)
        if (fragmentOldObject != null) {
            fragmentManager.popBackStackImmediate(tag, 0)
        } else {
            if (addToBackStack) {
                transaction?.addToBackStack(tag)
            }
            transaction?.add(id, fragment, tag)
                    ?.commitAllowingStateLoss()
        }
    }

    //for future use
    fun addFragmentForFlipTransition(fragment: Fragment, addToBackStack: Boolean, id: Int) {
        val tag = fragment.javaClass.simpleName
        val fragmentManager = mActivity?.supportFragmentManager
        val fragmentOldObject = fragmentManager?.findFragmentByTag(tag)
        val transaction = fragmentManager?.beginTransaction()
        transaction?.setCustomAnimations(R.anim.anim_in, R.anim.anim_out, R.animator.right_in, R.animator.right_out)
        if (fragmentOldObject != null) {
            fragmentManager.popBackStackImmediate(tag, 0)
        } else {
            if (addToBackStack) {
                transaction?.addToBackStack(tag)
            }
            transaction?.replace(id, fragment, tag)
                    ?.commitAllowingStateLoss()
        }
    }


    private fun goBack() {
        activity?.onBackPressed()
    }

    private fun showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = Dialog(mActivity!!, android.R.style.Theme_Translucent)
            mProgressDialog?.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            mProgressDialog?.setContentView(R.layout.loader_half__layout)
            mProgressDialog?.setCancelable(false)
        }
        mProgressDialog?.show()
    }

    private fun hideProgress() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog?.dismiss()
        }
    }

    fun showMessage(message: String) {
        Utils.showSnackbar(mContent, message)
    }


    fun showLoading(show: Boolean?) {
        if (show!!) showProgress() else hideProgress()
    }


    fun checkForPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermission()
                return false
            }
        }
        return true
    }


    fun isNetworkAvailable(content: View?): Boolean {
        val cm = content?.context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        if (netInfo != null && netInfo.isConnectedOrConnecting) {
            return true
        }

        return false
    }

    /**
     * This method will request permission
     */
    private fun requestPermission() {

        ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
                Constants.PERMISSION_REQUEST_CODE)

    }


    /**
     * This will show permission dialog
     */


    fun replaceFragment(fragment: Fragment, animate: Boolean, container: Int) {
        val tag: String = fragment::class.java.simpleName

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        if (animate) {
            transaction?.setCustomAnimations(R.anim.anim_in, R.anim.anim_out, R.anim.anim_in_reverse, R.anim.anim_out_reverse)

        }
        transaction?.replace(container, fragment, tag)
                ?.commitAllowingStateLoss()
    }

    fun checkForStoragePermission(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                return false
            }
        }
        return true
    }

    /**
     ************** get color code as per the current priority to show
     * pollyline color accordingly
     */

    fun handleFailure(error: Throwable) {
        ApiFailureTypes().getFailureMessage(error)
    }


    fun goBackWithDelay() {
        Handler().postDelayed({ goBack() }, Constants.HANDLER_DELAY_TIME)

    }


    /*****
     ****** show notificaiton count ********
     *
     */
    fun showNotificationCount(textView: TextView) {
        val notiCount = Preferences.prefs?.getValue(Constants.NOTI_COUNT, 0) ?: 0
        if (notiCount == 0) {
            textView.visibility = View.GONE
        } else {
            textView.visibility = View.VISIBLE
            if (notiCount > 99) {
                textView.text = "99+"
            } else {
                textView.text = notiCount.toString()
            }
        }

    }


}