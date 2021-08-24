package com.app.tourguide.ui.placedetail

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.util.SparseArray
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.app.tourguide.R
import com.app.tourguide.base_classes.BaseFragment
import com.app.tourguide.callBack.ItemSelectedListener
import com.app.tourguide.offlineWork.FileAdapterHebrew
import com.app.tourguide.ui.downloadPreview.pojomodel.DataAvailLang
import com.app.tourguide.ui.mapBox.MapBoxActivity
import com.app.tourguide.ui.placedetail.pojomodel.SpotsItem
import com.app.tourguide.ui.videoView.VideoViewActivity
import com.app.tourguide.utils.Constants
import com.app.tourguide.utils.MultiTextWatcher
import com.app.tourguide.utils.security.ApiFailureTypes
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.*
import kotlinx.android.synthetic.main.fragment_placedetails.*
import kotlinx.android.synthetic.main.show_vertification.*
import timber.log.Timber
import java.util.*


class PlacesDetailsFragment : BaseFragment(), ItemSelectedListener, FetchObserver<Download> {

    private var mViewModel: PlacesDetailViewModel? = null
    private var packageid: String = ""
    private var editTexts: Array<EditText>? = null
    private var otpDialog: Dialog? = null
    private var spots: List<SpotsItem>? = null

    private var availLangList: ArrayList<DataAvailLang> = ArrayList()
    private var fileAdapterHebrew: FileAdapterHebrew? = null
    private var request: Request? = null
    private var fetch: Fetch? = null

    private var downloadedPosition: Int = -1

    private var mTourWithorWithoutToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getSerializable(Constants.PACKAGE_ID)?.let {
            packageid = arguments?.getString(Constants.PACKAGE_ID)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mViewModel = ViewModelProviders.of(this).get(PlacesDetailViewModel::class.java)
        return inflater.inflate(R.layout.fragment_placedetails, container, false)
    }


    private fun hitApitoGetTourDetails() {
        if (!deviceToken().equals("") && !packageid.equals("")) {
            mViewModel!!.getPlacesDetails(packageid, deviceToken())
            mViewModel!!.getAvailLang(packageid, deviceToken())
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachObserversToGetPlacesDetails()
        hitApitoGetTourDetails()

        rvTourPreviewList.layoutManager = LinearLayoutManager(activity)
        fetch = Fetch.getDefaultInstance()

        tv_map.setOnClickListener {
        }

        tv_preview.setOnClickListener {
            rvTourPreviewList.visibility = View.VISIBLE
            if (availLangList[0].fileType == "internet") {
                availLangList[0].status = "View"
                fileAdapterHebrew!!.notifyItemChanged(0)
            }
        }

        iv_close.setOnClickListener {
            fragmentManager?.popBackStack()
        }

        tv_entercode.setOnClickListener {
            //showAlertToValidation()
            if (mTourWithorWithoutToken.equals("with_token")) {
                showAlertToValidation()
            } else if (mTourWithorWithoutToken.equals("without_token")) {
                val intent = Intent(activity, MapBoxActivity::class.java)
                startActivity(intent.apply {
                    putExtra("packageId", packageid)
                    putExtra("packageTokenId", "")
                    putExtra("comingFrom", "place_detail_fragment")
                    putExtra("doubleTourStatus", false)
                })


            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun attachObserversToGetPlacesDetails() {
        mViewModel?.otpResponse?.observe(this, androidx.lifecycle.Observer {
            it?.let {
                if (it.status == 0) {
                    if (it.message?.contains("previously", true)!!)
                        Toast.makeText(activity, getString(R.string.txt_password_used), Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(activity, getString(R.string.txt_password_incorrrect), Toast.LENGTH_SHORT).show()
                }

                if (it.status == 1) {
                    otpDialog?.dismiss()
                    val intent = Intent(activity, MapBoxActivity::class.java)
                    if (mTourWithorWithoutToken.equals("with_token")) {
                        startActivity(intent.apply {
                            putExtra("packageId", packageid)
                            putExtra("packageTokenId", it.data?.id)
                            putExtra("comingFrom", "place_detail_fragment")
                            putExtra("doubleTourStatus", true)
                        })
                    } else if (mTourWithorWithoutToken.equals("without_token")) {
                        //double tour prevention will be turn off
                        startActivity(intent.apply {
                            putExtra("packageId", packageid)
                            putExtra("packageTokenId", "")
                            putExtra("comingFrom", "place_detail_fragment")
                            putExtra("doubleTourStatus", false)
                        })
                    }
                }
            }
        })

        mViewModel?.tourPreviewResponse?.observe(this, androidx.lifecycle.Observer {
            it?.let {
                if (it.status == 1) {
                    availLangList = it.data as ArrayList<DataAvailLang>
                    fileAdapterHebrew = FileAdapterHebrew(availLangList, activity)
                    fileAdapterHebrew?.ItemSelectedListener(this)
                    rvTourPreviewList.adapter = fileAdapterHebrew


                }
            }
        })


        mViewModel?.response?.observe(this, androidx.lifecycle.Observer {
            it?.let {
                if (it.status == 1) {
                    if (it.data?.spots != null) {
                        tvTotalVideos.text = it.data.spots.size.toString() + " Videos"
                        spots = it.data.spots

                        if (it.data.tourTime?.let { it1 -> Integer.parseInt(it1) }!! > 10000) {
                            tv_entercode.text = getString(R.string.download_tour)
                            mTourWithorWithoutToken = "without_token"

                        } else {
                            tv_entercode.text = getString(R.string.enter_code_to_download)
                            mTourWithorWithoutToken = "with_token"
                        }

                        /*mTourWithorWithoutToken = when (Integer.parseInt(it.data.tourTime) > 10000) {
                            true -> "without_token"
                            false -> "with_token"
                        }*/

                        //tv_entercode.text = getString(R.string.enter_code_to_download)
                        tv_placename.visibility = View.VISIBLE
                        rlDescription.visibility = View.VISIBLE
                        tv_placeDescription.text = it.data.pDescription
                        tv_placename.text = it.data.pName
                        tvAvailLang.text = it.data.availableIn
                        Glide.with(activity).load(it.data.pImage).apply(RequestOptions().placeholder(R.mipmap.detail_bg)).into(iv_pckg_img)
                    }


                }
            }
        })

        mViewModel?.apiError?.observe(this, androidx.lifecycle.Observer {
            it?.let {
                showSnackBar(it)
            }
        })

        mViewModel?.isLoading?.observe(this, androidx.lifecycle.Observer {
            it?.let { showLoading(it) }
        })

        mViewModel?.onFailure?.observe(this, androidx.lifecycle.Observer {
            it?.let {
                showSnackBar(ApiFailureTypes().getFailureMessage(it))
            }
        })
    }


    private fun showAlertToValidation() {
        val mDialogForVer = Dialog(activity!!, R.style.PauseDialog)
        mDialogForVer.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        mDialogForVer.setContentView(R.layout.show_vertification)
        mDialogForVer.setCancelable(false)
        otpDialog = mDialogForVer

        editTexts = arrayOf(mDialogForVer.ed1, mDialogForVer.ed2, mDialogForVer.ed3, mDialogForVer.ed4, mDialogForVer.ed5, mDialogForVer.ed6, mDialogForVer.ed7,
                mDialogForVer.ed8)

        MultiTextWatcher()
                .registerEditText(mDialogForVer.ed1)
                .registerEditText(mDialogForVer.ed2)
                .registerEditText(mDialogForVer.ed3)
                .registerEditText(mDialogForVer.ed4)
                .registerEditText(mDialogForVer.ed5)
                .registerEditText(mDialogForVer.ed6)
                .registerEditText(mDialogForVer.ed7)
                .registerEditText(mDialogForVer.ed8)
                .setCallback(object : MultiTextWatcher.TextWatcherWithInstance {
                    override fun beforeTextChanged(editText: EditText, s: CharSequence, start: Int, count: Int, after: Int) {

                    }

                    override fun onTextChanged(editText: EditText, s: CharSequence, start: Int, before: Int, count: Int) {
                        when (editText) {
                            mDialogForVer.ed1 -> if (mDialogForVer.ed1.text.length == 1) {
                                mDialogForVer.ed2.requestFocus()
                                mDialogForVer.ed1.setOnKeyListener(PinOnKeyListener(0))
                            }
                            mDialogForVer.ed2 -> if (mDialogForVer.ed2.text.length == 1) {
                                mDialogForVer.ed3.requestFocus()
                                mDialogForVer.ed2.setOnKeyListener(PinOnKeyListener(1))
                            }
                            mDialogForVer.ed3 -> if (mDialogForVer.ed3.text.length == 1) {
                                mDialogForVer.ed4.requestFocus()
                                mDialogForVer.ed3.setOnKeyListener(PinOnKeyListener(2))
                            }
                            mDialogForVer.ed4 -> if (mDialogForVer.ed4.text.length == 1) {
                                mDialogForVer.ed5.requestFocus()
                                mDialogForVer.ed4.setOnKeyListener(PinOnKeyListener(3))
                            }
                            mDialogForVer.ed5 -> if (mDialogForVer.ed5.text.length == 1) {
                                mDialogForVer.ed6.requestFocus()
                                mDialogForVer.ed5.setOnKeyListener(PinOnKeyListener(4))
                            }

                            mDialogForVer.ed6 -> if (mDialogForVer.ed6.text.length == 1) {
                                mDialogForVer.ed7.requestFocus()
                                mDialogForVer.ed6.setOnKeyListener(PinOnKeyListener(5))
                            }


                            mDialogForVer.ed7 -> if (mDialogForVer.ed7.text.length == 1) {
                                mDialogForVer.ed8.requestFocus()
                                mDialogForVer.ed7.setOnKeyListener(PinOnKeyListener(6))
                            }


                            mDialogForVer.ed8 -> if (mDialogForVer.ed8.text.length == 1) {
                                mDialogForVer.ed8.setOnKeyListener(PinOnKeyListener(7))
                            }
                            else -> {
                                return
                            }
                        }
                    }

                    override fun afterTextChanged(editText: EditText, editable: Editable) {

                    }
                })

        mDialogForVer.btn_submit.setOnClickListener {
            val e1: String = mDialogForVer.ed1.text.toString().trim()
            val e2: String = mDialogForVer.ed2.text.toString().trim()
            val e3: String = mDialogForVer.ed3.text.toString().trim()
            val e4: String = mDialogForVer.ed4.text.toString().trim()
            val e5: String = mDialogForVer.ed5.text.toString().trim()
            val e6: String = mDialogForVer.ed6.text.toString().trim()
            val e7: String = mDialogForVer.ed7.text.toString().trim()
            val e8: String = mDialogForVer.ed8.text.toString().trim()

            val combineOtp = e1 + e2 + e3 + e4 + e5 + e6 + e7 + e8

            if (!e1.isEmpty() && !e2.isEmpty() && !e3.isEmpty() && !e4.isEmpty() && !e5.isEmpty()
                    && !e6.isEmpty() && !e7.isEmpty() && !e8.isEmpty()) {
                hitOtpApi(combineOtp)
            } else {
                Toast.makeText(activity, "Please enter the valid code", Toast.LENGTH_SHORT).show()
            }

        }

        mDialogForVer.iv_cancel.setOnClickListener {
            mDialogForVer.dismiss()
        }
        mDialogForVer.show()

    }

    private fun enqueueDownload(url: String) {
        val url = url
        val filePath = getSaveDir() + "/movies/" + getNameFromUrl(url)

        request = Request(url, filePath)
        request!!.extras = getExtrasForRequest(request!!)

        fetch!!.attachFetchObserversForDownload(request!!.id, this)
                .enqueue(request!!, Func { result -> request = result }, Func { result -> Timber.d("SingleDownloadActivity Error: %1\$s", result.toString()) })


    }


    fun getSaveDir(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/fetch"
    }


    internal fun getNameFromUrl(url: String): String {
        return Uri.parse(url).lastPathSegment!!
    }

    private fun getExtrasForRequest(request: Request): Extras {
        val extras = MutableExtras()
        extras.putBoolean("testBoolean", true)
        extras.putString("testString", "test")
        extras.putFloat("testFloat", java.lang.Float.MIN_VALUE)
        extras.putDouble("testDouble", java.lang.Double.MIN_VALUE)
        extras.putInt("testInt", Integer.MAX_VALUE)
        extras.putLong("testLong", java.lang.Long.MAX_VALUE)
        return extras
    }


    private fun hitOtpApi(code: String) {
        if (!deviceToken().equals(""))
            mViewModel!!.sendOtp(code, deviceToken(), packageid)
    }

    inner class PinOnKeyListener internal constructor(private val currentIndex: Int) : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action === KeyEvent.ACTION_DOWN) {
                if (editTexts!![currentIndex].text.toString().isEmpty() && currentIndex != 0)
                    editTexts!![currentIndex - 1].requestFocus()
            }
            return false
        }
    }


    override fun onChanged(download: Download, reason: Reason) {
        //update view here
        try {
            if (request!!.id == download.id) {
                if (reason == Reason.DOWNLOAD_QUEUED || reason == Reason.DOWNLOAD_COMPLETED) {
                    // setTitleView(download.file)
                }

                when (download.status) {
                    Status.DOWNLOADING, Status.COMPLETED -> {
                        if (download.progress == -1) {
                            availLangList[downloadedPosition].status = getString(R.string.txt_downloading)
                        } else {
                            availLangList[downloadedPosition].progress = download.progress

                            if (download.progress == 100) {
                                availLangList[downloadedPosition].status = getString(R.string.txt_view)
                            }
                        }
                    }
                    else -> {
                    }
                }

                fileAdapterHebrew?.notifyItemChanged(downloadedPosition)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun selectedItem(pos: Int, type: String, uri: Uri) {
        if (availLangList[pos].fileType != null) {
            when {
                availLangList[pos].fileType.equals("video", true) -> {
                    if (isNetworkAvailable(view)) {
                        if (type == Constants.VIEW_VIDEO) {
                            playTourPreviewVideo(uri.toString())
                        } else {
                            downloadedPosition = pos
                            enqueueDownload(availLangList[pos].videoUrl.toString())
                        }
                    } else {
                        showSnackBar(getString(R.string.no_internet_message))
                    }
                }
                availLangList[pos].fileType.equals("internet", true) -> {
                    if (isNetworkAvailable(view)) {
                        when {
                            availLangList[pos].internetLink!!.contains("youtube") -> getYoutubePlayingLink(availLangList[pos].internetLink)
                            else -> playTourPreviewVideo(availLangList[pos].internetLink!!)
                        }
                    } else {
                        showSnackBar(getString(R.string.no_internet_message))
                    }
                }
                else -> showMessage(getString(R.string.preview_video_missing))
            }

        }

        /* if (type == Constants.VIEW_VIDEO) {
             val url = uri.toString()
             if (url.contains("youtube")) {
                 getYoutubePlayingLink(url)
             } else {
                 playTourPreviewVideo(url)
             }
         } else {
             if (isNetworkAvailable(view)) {
                 downloadedPosition = pos
                 enqueueDownload(availLangList[pos].videoUrl.toString())
             } else {
                 showSnackBar(getString(R.string.no_internet_message))
             }
         }*/
    }


    private fun getYoutubePlayingLink(youtubeId: String?) {
        @SuppressLint("StaticFieldLeak") val mExtractor = object : YouTubeExtractor(this.activity!!) {
            override fun onExtractionComplete(sparseArray: SparseArray<YtFile>?, videoMeta: VideoMeta) {
                if (sparseArray != null) {
                    playTourPreviewVideo(sparseArray.get(18).url)
                } else {
                    showMessage("This video is no longer available on youtube")
                }
            }
        }
        mExtractor.extract(youtubeId, true, true)
    }

    private fun playTourPreviewVideo(url: String) {
        /*val args: Bundle? = Bundle()
        args?.apply {
            putString("video_url", url)
        }
        val fragment = VideoViewFragment()
        fragment.arguments = args
        addFragment(fragment, true, R.id.container_main)
        */
        val intent = Intent(activity, VideoViewActivity::class.java)
        intent.putExtra(Constants.VIDEO_URL, url)
        startActivity(intent)

    }


}


